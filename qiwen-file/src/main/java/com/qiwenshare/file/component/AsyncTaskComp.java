package com.qiwenshare.file.component;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.api.IFiletransferService;
import com.qiwenshare.file.api.IRecoveryFileService;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.mapper.FileMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.copy.domain.CopyFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * 功能描述：异步任务业务类（@Async也可添加在方法上）
 */
@Slf4j
@Component
@Async("asyncTaskExecutor")
public class AsyncTaskComp
{

    @Resource
    private IRecoveryFileService recoveryFileService;
    @Resource
    private IFiletransferService filetransferService;
    @Resource
    private UFOPFactory ufopFactory;
    @Resource
    private UserFileMapper userFileMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileDealComp fileDealComp;

    @Value("${ufop.storage-type}")
    private Integer storageType;

    public Long getFilePointCount(String fileId) {
        return userFileMapper.selectCount(new LambdaQueryWrapper<UserFile>().eq(UserFile::getFileId, fileId));
    }

    public Future<String> deleteUserFile(String userFileId) {
        // 查询用户文件
        UserFile userFile = userFileMapper.selectById(userFileId);
        // 1、目录
        if (userFile.isDirectory()) {
            // 删除回收站文件
            recoveryFileService.deleteUserFileByDeleteBatchNum(userFile.getDeleteBatchNum());
            // 删除目录下的所有文件
            List<UserFile> list = userFileMapper.selectList(new LambdaQueryWrapper<UserFile>().eq(UserFile::getDeleteBatchNum, userFile.getDeleteBatchNum()));
            for (UserFile userFileItem : list) {
                // 根据文件ID查询文件数
                Long filePointCount = getFilePointCount(userFileItem.getFileId());
                // 文件点数为0，删除文件
                if (Objects.nonNull(filePointCount) && filePointCount == 0 && userFileItem.isFile()) {
                    FileBean fileBean = fileMapper.selectById(userFileItem.getFileId());
                    if (Objects.nonNull(fileBean)) {
                        try {
                            filetransferService.deleteFile(fileBean);
                            fileMapper.deleteById(fileBean.getFileId());
                        }
                        catch (Exception e) {
                            log.error("删除本地文件失败：" + JSON.toJSONString(fileBean));
                        }
                    }
                }
            }
        }
        // 2、文件
        else {
            // 删除回收站文件
            recoveryFileService.deleteUserFileByDeleteBatchNum(userFile.getDeleteBatchNum());
            // 根据文件ID查询文件数
            Long filePointCount = getFilePointCount(userFile.getFileId());
            // 文件点数为0，删除文件
            if (Objects.nonNull(filePointCount) && filePointCount == 0 && userFile.isFile()) {
                FileBean fileBean = fileMapper.selectById(userFile.getFileId());
                try {
                    filetransferService.deleteFile(fileBean);
                    fileMapper.deleteById(fileBean.getFileId());
                }
                catch (Exception e) {
                    log.error("删除本地文件失败：" + JSON.toJSONString(fileBean));
                }
            }
        }

        return new AsyncResult<>("deleteUserFile");
    }

    public Future<String> checkESUserFileId(String userFileId) {
        UserFile userFile = userFileMapper.selectById(userFileId);
        if (Objects.isNull(userFile)) {
            fileDealComp.deleteESByUserFileId(userFileId);
        }
        return new AsyncResult<>("checkUserFileId");
    }

    public Future<String> saveUnzipFile(UserFile userFile, FileBean fileBean, int unzipMode, String entryName, String filePath) {
        String unzipUrl = UFOPUtils
                .getTempFile(fileBean.getFileUrl())
                .getAbsolutePath()
                .replace("." + userFile.getExtendName(), "");
        String totalFileUrl = unzipUrl + entryName;
        File currentFile = new File(totalFileUrl);

        String fileId = null;
        if (!currentFile.isDirectory()) {
            // 获取文件MD5
            String md5Str = UUID
                    .randomUUID()
                    .toString();
            try (FileInputStream fis = new FileInputStream(currentFile)) {
                md5Str = DigestUtils.md5Hex(fis);
            }
            catch (IOException e) {
                log.error("获取文件MD5失败：{}", e.getMessage(), e);
            }

            try {
                Map<String, Object> param = new HashMap<>();
                param.put("identifier", md5Str);
                List<FileBean> list = fileMapper.selectByMap(param);

                // 文件已存在
                if (CollectionUtils.isNotEmpty(list)) {
                    fileId = list
                            .get(0)
                            .getFileId();
                }
                // 文件不存在
                else {
                    CopyFile createFile = new CopyFile();
                    createFile.setExtendName(FilenameUtils.getExtension(totalFileUrl));
                    String saveFileUrl;
                    try (FileInputStream fileInputStream = new FileInputStream(currentFile)) {
                        saveFileUrl = ufopFactory
                                .getCopier()
                                .copy(fileInputStream, createFile);
                    }

                    // 保存文件
                    FileBean tempFileBean = new FileBean(saveFileUrl, currentFile.length(), storageType, md5Str, userFile.getUserId());
                    fileMapper.insert(tempFileBean);
                    fileId = tempFileBean.getFileId();
                }
            }
            catch (IOException e) {
                log.error("保存文件失败：{}", e.getMessage(), e);
            }
            finally {
                System.gc();
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentFile.delete();
            }
        }

        QiwenFile qiwenFile = null;
        if (unzipMode == 0) {
            qiwenFile = new QiwenFile(userFile.getFilePath(), entryName, currentFile.isDirectory());
        }
        else if (unzipMode == 1) {
            qiwenFile = new QiwenFile(userFile.getFilePath() + "/" + userFile.getFileName(), entryName, currentFile.isDirectory());
        }
        else if (unzipMode == 2) {
            qiwenFile = new QiwenFile(filePath, entryName, currentFile.isDirectory());
        }

        UserFile saveUserFile = new UserFile(qiwenFile, userFile.getUserId(), fileId);
        String fileName = fileDealComp.getRepeatFileName(saveUserFile, saveUserFile.getFilePath());

        //如果是目录，而且重复，什么也不做
        if (!saveUserFile.isDirectory() || !fileName.equals(saveUserFile.getFileName())) {
            saveUserFile.setFileName(fileName);
            userFileMapper.insert(saveUserFile);
        }
        fileDealComp.restoreParentFilePath(qiwenFile, userFile.getUserId());

        return new AsyncResult<>("saveUnzipFile");
    }

}
