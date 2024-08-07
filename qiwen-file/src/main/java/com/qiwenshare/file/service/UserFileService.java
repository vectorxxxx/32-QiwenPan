package com.qiwenshare.file.service;

import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.common.constant.FileConstant;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IUserFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.domain.RecoveryFile;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.dto.file.CreateFileDTO;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.mapper.RecoveryFileMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.file.vo.file.FileListVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class UserFileService extends ServiceImpl<UserFileMapper, UserFile> implements IUserFileService
{
    @Resource
    UserFileMapper userFileMapper;
    @Resource
    RecoveryFileMapper recoveryFileMapper;
    @Resource
    FileDealComp fileDealComp;

    public static Executor executor = Executors.newFixedThreadPool(20);

    @Override
    public List<UserFile> selectUserFileByNameAndPath(String fileName, String filePath, String userId) {
        LambdaQueryWrapper<UserFile> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(UserFile::getFileName, fileName)
                .eq(UserFile::getFilePath, filePath)
                .eq(UserFile::getUserId, userId)
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag());
        return userFileMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public List<UserFile> selectSameUserFile(CreateFileDTO createFileDTO, String userId) {
        return userFileMapper.selectList(new LambdaQueryWrapper<UserFile>()
                .eq(UserFile::getUserId, userId)
                .eq(UserFile::getFilePath, createFileDTO.getFilePath())
                .eq(UserFile::getFileName, createFileDTO.getFileName())
                .eq(UserFile::getExtendName, createFileDTO.getExtendName())
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
    }

    @Override
    public IPage<FileListVO> userFileList(String userId, String filePath, Long currentPage, Long pageCount) {
        Page<FileListVO> page = new Page<>(currentPage, pageCount);
        UserFile userFile = new UserFile()
                .setUserId(Objects.isNull(userId) ?
                           SessionUtil.getUserId() :
                           userId)
                .setFilePath(URLDecoder.decodeForPath(filePath, StandardCharsets.UTF_8));

        return userFileMapper.selectPageVo(page, userFile, null);
    }

    @Override
    public void updateFilepathByUserFileId(String userFileId, String newFilePath, String userId) {
        final UserFile userFile = userFileMapper.selectById(userFileId);
        final String fileName = userFile.getFileName();
        String oldFilePath = userFile.getFilePath();

        userFile.setFilePath(newFilePath);
        if (userFile.isFile()) {
            String repeatFileName = fileDealComp.getRepeatFileName(userFile, userFile.getFilePath());
            userFile.setFileName(repeatFileName);
        }
        try {
            userFileMapper.updateById(userFile);
        }
        catch (Exception e) {
            log.warn(e.getMessage());
        }

        // 移动子目录
        oldFilePath = new QiwenFile(oldFilePath, fileName, true).getPath();
        newFilePath = new QiwenFile(newFilePath, fileName, true).getPath();
        // 如果是目录，则需要移动子目录
        if (userFile.isDirectory()) {
            List<UserFile> list = selectUserFileByLikeRightFilePath(oldFilePath, userId);
            for (UserFile newUserFile : list) {
                newUserFile.setFilePath(newUserFile
                        .getFilePath()
                        .replaceFirst(oldFilePath, newFilePath));
                if (newUserFile.isDirectory()) {
                    String repeatFileName = fileDealComp.getRepeatFileName(newUserFile, newUserFile.getFilePath());
                    newUserFile.setFileName(repeatFileName);
                }
                try {
                    userFileMapper.updateById(newUserFile);
                }
                catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }

    }

    @Override
    public void userFileCopy(String userId, String userFileId, String newfilePath) {
        final UserFile userFile = userFileMapper.selectById(userFileId);
        final String oldUserId = userFile.getUserId();
        final String fileName = userFile.getFileName();
        String oldFilePath = userFile.getFilePath();

        userFile
                .setFilePath(newfilePath)
                .setUserId(userId)
                .setUserFileId(IdUtil.getSnowflakeNextIdStr());
        if (userFile.isDirectory()) {
            String repeatFileName = fileDealComp.getRepeatFileName(userFile, userFile.getFilePath());
            userFile.setFileName(repeatFileName);
        }
        try {
            userFileMapper.insert(userFile);
        }
        catch (Exception e) {
            log.warn(e.getMessage());
        }

        oldFilePath = new QiwenFile(oldFilePath, fileName, true).getPath();
        newfilePath = new QiwenFile(newfilePath, fileName, true).getPath();

        if (userFile.isDirectory()) {
            List<UserFile> subUserFileList = userFileMapper.selectUserFileByLikeRightFilePath(oldFilePath, oldUserId);

            for (UserFile newUserFile : subUserFileList) {
                newUserFile.setFilePath(newUserFile
                        .getFilePath()
                        .replaceFirst(oldFilePath, newfilePath));
                newUserFile.setUserFileId(IdUtil.getSnowflakeNextIdStr());
                if (newUserFile.isDirectory()) {
                    String repeatFileName = fileDealComp.getRepeatFileName(newUserFile, newUserFile.getFilePath());
                    newUserFile.setFileName(repeatFileName);
                }
                newUserFile.setUserId(userId);
                try {
                    userFileMapper.insert(newUserFile);
                }
                catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
        }

    }

    @Override
    public IPage<FileListVO> getFileByFileType(Integer fileTypeId, Long currentPage, Long pageCount, String userId) {
        Page<FileListVO> page = new Page<>(currentPage, pageCount);

        UserFile userFile = new UserFile();
        userFile.setUserId(userId);
        return userFileMapper.selectPageVo(page, userFile, fileTypeId);
    }

    @Override
    public List<UserFile> selectUserFileListByPath(String filePath, String userId) {
        LambdaQueryWrapper<UserFile> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(UserFile::getFilePath, filePath)
                .eq(UserFile::getUserId, userId)
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag());
        return userFileMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public List<UserFile> selectFilePathTreeByUserId(String userId) {
        LambdaQueryWrapper<UserFile> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(UserFile::getUserId, userId)
                .eq(UserFile::getIsDir, 1)
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag());
        return userFileMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public void deleteUserFile(String userFileId, String sessionUserId) {
        final UserFile userFile = userFileMapper.selectById(userFileId);
        final String uuid = UUID
                .randomUUID()
                .toString();
        final String currentTime = DateUtil.getCurrentTime();

        // 删除文件夹
        if (userFile.isDirectory()) {
            userFileMapper.update(null, new LambdaUpdateWrapper<UserFile>()
                    .set(UserFile::getDeleteFlag, RandomUtil.randomInt(FileConstant.deleteFileRandomSize))
                    .set(UserFile::getDeleteBatchNum, uuid)
                    .set(UserFile::getDeleteTime, currentTime)
                    .eq(UserFile::getUserFileId, userFileId));

            // 删除所有子文件
            String filePath = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true).getPath();
            updateFileDeleteStateByFilePath(filePath, uuid, sessionUserId);
        }
        // 删除文件
        else {
            userFileMapper.update(null, new LambdaUpdateWrapper<UserFile>()
                    .set(UserFile::getDeleteFlag, RandomUtil.randomInt(1, FileConstant.deleteFileRandomSize))
                    .set(UserFile::getDeleteTime, currentTime)
                    .set(UserFile::getDeleteBatchNum, uuid)
                    .eq(UserFile::getUserFileId, userFileId));
        }

        // 插入回收文件
        recoveryFileMapper.insert(new RecoveryFile()
                .setUserFileId(userFileId)
                .setDeleteTime(currentTime)
                .setDeleteBatchNum(uuid));
    }

    @Override
    public List<UserFile> selectUserFileByLikeRightFilePath(String filePath, String userId) {
        return userFileMapper.selectUserFileByLikeRightFilePath(filePath, userId);
    }

    private void updateFileDeleteStateByFilePath(String filePath, String deleteBatchNum, String userId) {
        executor.execute(() -> {
            List<String> userFileIds = selectUserFileByLikeRightFilePath(filePath, userId)
                    .stream()
                    .map(UserFile::getUserFileId)
                    .collect(Collectors.toList());

            //标记删除标志
            if (CollectionUtils.isNotEmpty(userFileIds)) {
                userFileMapper.update(null, new LambdaUpdateWrapper<UserFile>()
                        .set(UserFile::getDeleteFlag, RandomUtil.randomInt(FileConstant.deleteFileRandomSize))
                        .set(UserFile::getDeleteTime, DateUtil.getCurrentTime())
                        .set(UserFile::getDeleteBatchNum, deleteBatchNum)
                        .in(UserFile::getUserFileId, userFileIds)
                        .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
            }
            for (String userFileId : userFileIds) {
                fileDealComp.deleteESByUserFileId(userFileId);
            }
        });
    }

}
