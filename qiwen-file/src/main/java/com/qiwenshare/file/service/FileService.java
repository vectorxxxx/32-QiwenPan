package com.qiwenshare.file.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.common.exception.QiwenException;
import com.qiwenshare.common.operation.FileOperation;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.component.AsyncTaskComp;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.Image;
import com.qiwenshare.file.domain.Music;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.mapper.FileMapper;
import com.qiwenshare.file.mapper.ImageMapper;
import com.qiwenshare.file.mapper.MusicMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.file.util.QiwenFileUtil;
import com.qiwenshare.file.vo.file.FileDetailVO;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FileService extends ServiceImpl<FileMapper, FileBean> implements IFileService
{
    public static Executor executor = Executors.newFixedThreadPool(20);

    @Resource
    private FileMapper fileMapper;
    @Resource
    private UserFileMapper userFileMapper;
    @Resource
    private UFOPFactory ufopFactory;

    @Resource
    private AsyncTaskComp asyncTaskComp;
    @Resource
    private MusicMapper musicMapper;
    @Resource
    private ImageMapper imageMapper;
    @Resource
    private FileDealComp fileDealComp;

    @Value("${ufop.storage-type}")
    private Integer storageType;

    @Override
    public Long getFilePointCount(String fileId) {
        LambdaQueryWrapper<UserFile> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserFile::getFileId, fileId);
        return userFileMapper.selectCount(lambdaQueryWrapper);
    }

    @Override
    public void unzipFile(String userFileId, int unzipMode, String filePath) {
        // 获取文件信息
        UserFile userFile = userFileMapper.selectById(userFileId);
        // 获取文件元数据
        FileBean fileBean = fileMapper.selectById(userFile.getFileId());
        File destFile = new File(UFOPUtils
                .getStaticPath()
                .concat("temp")
                .concat(File.separator)
                .concat(fileBean.getFileUrl()));

        // 下载文件
        Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.setFileUrl(fileBean.getFileUrl());
        InputStream inputStream = downloader.getInputStream(downloadFile);
        try {
            FileUtils.copyInputStreamToFile(inputStream, destFile);
        }
        catch (IOException e) {
            log.error("下载失败：{}", e.getMessage(), e);
        }

        String extendName = userFile.getExtendName();
        String unzipUrl = UFOPUtils
                .getTempFile(fileBean.getFileUrl())
                .getAbsolutePath()
                .replace("." + extendName, "");

        // 解压
        List<String> fileEntryNameList;
        try {
            fileEntryNameList = FileOperation.unzip(destFile, unzipUrl);
        }
        catch (Exception e) {
            log.error("解压失败：{}", e.getMessage(), e);
            throw new QiwenException(500001, "解压异常：" + e.getMessage());
        }

        if (destFile.exists()) {
            destFile.delete();
        }

        // 当文件条目列表不为空且解压模式为1时，创建并保存一个代表Qiwen目录的UserFile对象
        if (CollectionUtils.isNotEmpty(fileEntryNameList) && unzipMode == 1) {
            UserFile qiwenDir = QiwenFileUtil.getQiwenDir(userFile.getUserId(), userFile.getFilePath(), userFile.getFileName());
            userFileMapper.insert(qiwenDir);
        }

        // 遍历文件条目列表，对于每个条目，异步保存解压后的文件
        for (int i = 0; i < fileEntryNameList.size(); i++) {
            String entryName = fileEntryNameList.get(i);
            asyncTaskComp.saveUnzipFile(userFile, fileBean, unzipMode, entryName, filePath);
        }
    }

    @Override
    public void updateFileDetail(String userFileId, String identifier, long fileSize) {
        final UserFile userFile = userFileMapper.selectById(userFileId);
        final String currentTime = DateUtil.getCurrentTime();
        final String userId = SessionUtil.getUserId();

        // 更新file
        fileMapper.updateById(new FileBean()
                .setIdentifier(identifier)
                .setFileSize(fileSize)
                .setModifyTime(currentTime)
                .setModifyUserId(userId)
                .setFileId(userFile.getFileId()));

        // 更新userFile
        userFileMapper.updateById(userFile
                .setUploadTime(currentTime)
                .setModifyTime(currentTime)
                .setModifyUserId(userId));
    }

    @Override
    public FileDetailVO getFileDetail(String userFileId) {
        // 获取userFile
        final UserFile userFile = userFileMapper.selectById(userFileId);
        // 获取file
        final FileBean fileBean = fileMapper.selectById(userFile.getFileId());
        // 获取music
        Music music = musicMapper.selectOne(new LambdaQueryWrapper<Music>().eq(Music::getFileId, userFile.getFileId()));
        // 获取image
        final Image image = imageMapper.selectOne(new LambdaQueryWrapper<Image>().eq(Image::getFileId, userFile.getFileId()));

        // 如果是音乐文件，则解析音乐文件
        final boolean isMusic = "mp3".equalsIgnoreCase(userFile.getExtendName()) || "flac".equalsIgnoreCase(userFile.getExtendName());
        if (isMusic && (Objects.isNull(music))) {
            fileDealComp.parseMusicFile(userFile.getExtendName(), fileBean.getStorageType(), fileBean.getFileUrl(), fileBean.getFileId());
            music = musicMapper.selectOne(new QueryWrapper<Music>().eq("fileId", userFile.getFileId()));
        }

        // 封装VO
        FileDetailVO fileDetailVO = new FileDetailVO();
        BeanUtil.copyProperties(userFile, fileDetailVO);
        BeanUtil.copyProperties(fileBean, fileDetailVO);
        fileDetailVO.setMusic(music);
        fileDetailVO.setImage(image);
        return fileDetailVO;
    }

}
