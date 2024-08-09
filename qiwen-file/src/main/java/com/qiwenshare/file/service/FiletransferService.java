package com.qiwenshare.file.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.MimeUtils;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IFiletransferService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.Image;
import com.qiwenshare.file.domain.PictureFile;
import com.qiwenshare.file.domain.UploadTask;
import com.qiwenshare.file.domain.UploadTaskDetail;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.dto.file.DownloadFileDTO;
import com.qiwenshare.file.dto.file.PreviewDTO;
import com.qiwenshare.file.dto.file.UploadFileDTO;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.mapper.FileMapper;
import com.qiwenshare.file.mapper.ImageMapper;
import com.qiwenshare.file.mapper.PictureFileMapper;
import com.qiwenshare.file.mapper.UploadTaskDetailMapper;
import com.qiwenshare.file.mapper.UploadTaskMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.file.vo.file.UploadFileVo;
import com.qiwenshare.ufop.constant.StorageTypeEnum;
import com.qiwenshare.ufop.constant.UploadFileStatusEnum;
import com.qiwenshare.ufop.exception.operation.DownloadException;
import com.qiwenshare.ufop.exception.operation.UploadException;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.delete.Deleter;
import com.qiwenshare.ufop.operation.delete.domain.DeleteFile;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.operation.preview.domain.PreviewFile;
import com.qiwenshare.ufop.operation.upload.Uploader;
import com.qiwenshare.ufop.operation.upload.domain.UploadFile;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FiletransferService implements IFiletransferService
{
    public static Executor exec = Executors.newFixedThreadPool(20);

    @Resource
    private FileMapper fileMapper;

    @Resource
    private UserFileMapper userFileMapper;

    @Resource
    private UFOPFactory ufopFactory;
    @Resource
    private FileDealComp fileDealComp;
    @Resource
    private UploadTaskDetailMapper uploadTaskDetailMapper;
    @Resource
    private UploadTaskMapper uploadTaskMapper;
    @Resource
    private ImageMapper imageMapper;

    @Resource
    private PictureFileMapper pictureFileMapper;

    @Override
    public UploadFileVo uploadFileSpeed(UploadFileDTO uploadFileDTO) {
        UploadFileVo uploadFileVo = new UploadFileVo();

        final String userId = SessionUtil.getUserId();
        final String filePath = uploadFileDTO.getFilePath();
        final String relativePath = uploadFileDTO.getRelativePath();
        QiwenFile qiwenFile = new QiwenFile(filePath, relativePath.contains("/") ?
                                                      relativePath :
                                                      uploadFileDTO.getFilename(), false);

        // 根据md5唯一标识查询文件
        final String identifier = uploadFileDTO.getIdentifier();
        final FileBean fileBean = fileMapper.selectOne(new LambdaQueryWrapper<FileBean>().eq(FileBean::getIdentifier, identifier));
        // 如果文件存在，则跳过上传
        if (Objects.nonNull(fileBean)) {
            UserFile userFile = new UserFile(qiwenFile, userId, fileBean.getFileId());

            try {
                // 插入用户文件表
                userFileMapper.insert(userFile);
                // 更新 ES 索引
                fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
            }
            catch (Exception e) {
                log.warn("极速上传文件冲突重命名处理: {}", JSON.toJSONString(userFile));
            }

            if (relativePath.contains("/")) {
                exec.execute(() -> fileDealComp.restoreParentFilePath(qiwenFile, userId));
            }
            uploadFileVo.setSkipUpload(true);
        }
        // 如果文件不存在，则上传
        else {
            uploadFileVo.setSkipUpload(false);

            // 查询已上传的分片
            List<Integer> uploaded = uploadTaskDetailMapper.selectUploadedChunkNumList(identifier);
            // 如果已上传分片不为空，则设置已上传分片
            if (CollectionUtils.isNotEmpty(uploaded)) {
                uploadFileVo.setUploaded(uploaded);
            }
            // 如果已上传分片为空，则插入上传任务表
            else {
                List<UploadTask> rslist = uploadTaskMapper.selectList(new LambdaQueryWrapper<UploadTask>().eq(UploadTask::getIdentifier, identifier));
                if (CollectionUtils.isEmpty(rslist)) {
                    uploadTaskMapper.insert(new UploadTask()
                            .setIdentifier(identifier)
                            .setUserId(userId)
                            .setFileName(qiwenFile.getNameNotExtend())
                            .setFilePath(qiwenFile.getParent())
                            .setExtendName(qiwenFile.getExtendName())
                            .setUploadTime(DateUtil.getCurrentTime())
                            .setUploadStatus(UploadFileStatusEnum.UNCOMPLATE.getCode()));
                }
            }
        }
        return uploadFileVo;
    }

    @Override
    public void uploadFile(HttpServletRequest request, UploadFileDTO uploadFileDto, String userId) {
        final String identifier = uploadFileDto.getIdentifier();

        // 封装上传文件对象
        UploadFile uploadFile = new UploadFile();
        uploadFile.setChunkNumber(uploadFileDto.getChunkNumber());  // 当前分片
        uploadFile.setChunkSize(uploadFileDto.getChunkSize());      // 分片大小
        uploadFile.setTotalChunks(uploadFileDto.getTotalChunks());  // 分片总数
        uploadFile.setIdentifier(identifier);    // md5
        uploadFile.setTotalSize(uploadFileDto.getTotalSize());      // 文件大小
        uploadFile.setCurrentChunkSize(uploadFileDto.getCurrentChunkSize());    // 当前分片大小

        // 获取上传器
        Uploader uploader = ufopFactory.getUploader();
        if (Objects.isNull(uploader)) {
            log.error("上传失败，请检查storageType是否配置正确");
            throw new UploadException("上传失败");
        }

        // 上传文件
        List<UploadFileResult> uploadFileResultList;
        try {
            uploadFileResultList = uploader.upload(request, uploadFile);
        }
        catch (Exception e) {
            log.error("上传失败，请检查UFOP连接配置是否正确");
            throw new UploadException("上传失败", e);
        }

        // 遍历上传结果
        for (UploadFileResult uploadFileResult : uploadFileResultList) {
            String relativePath = uploadFileDto.getRelativePath();
            QiwenFile qiwenFile = new QiwenFile(uploadFileDto.getFilePath(), relativePath.contains("/") ?
                                                                             relativePath :
                                                                             uploadFileDto.getFilename(), false);

            // 1、上传成功
            if (UploadFileStatusEnum.SUCCESS.equals(uploadFileResult.getStatus())) {
                // 插入文件表
                FileBean fileBean = new FileBean(uploadFileResult).setCreateUserId(userId);
                final String fileBeanIdentifier = fileBean.getIdentifier();
                try {
                    fileMapper.insert(fileBean);
                }
                catch (Exception e) {
                    log.warn("identifier Duplicate: {}", fileBeanIdentifier);
                    fileBean = fileMapper.selectOne(new LambdaQueryWrapper<FileBean>().eq(FileBean::getIdentifier, fileBeanIdentifier));
                }

                // 插入用户文件表，更新索引
                UserFile userFile = new UserFile(qiwenFile, userId, fileBean.getFileId());
                try {
                    userFileMapper.insert(userFile);
                    fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
                }
                catch (Exception e) {
                    UserFile userFile1 = userFileMapper.selectOne(new LambdaQueryWrapper<UserFile>()
                            .eq(UserFile::getUserId, userFile.getUserId())
                            .eq(UserFile::getFilePath, userFile.getFilePath())
                            .eq(UserFile::getFileName, userFile.getFileName())
                            .eq(UserFile::getExtendName, userFile.getExtendName())
                            .eq(UserFile::getDeleteFlag, userFile.getDeleteFlag())
                            .eq(UserFile::getIsDir, userFile.getIsDir()));
                    FileBean file1 = fileMapper.selectById(userFile1.getFileId());
                    if (!StringUtils.equals(fileBeanIdentifier, file1.getIdentifier())) {
                        log.warn("文件冲突重命名处理: {}", JSON.toJSONString(userFile1));
                        String fileName = fileDealComp.getRepeatFileName(userFile, userFile.getFilePath());
                        userFile.setFileName(fileName);
                        userFileMapper.insert(userFile);
                        fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
                    }
                }

                // 创建父级目录
                if (relativePath.contains("/")) {
                    exec.execute(() -> fileDealComp.restoreParentFilePath(qiwenFile, userId));
                }

                // 更新上传任务状态
                uploadTaskDetailMapper.delete(new LambdaQueryWrapper<UploadTaskDetail>().eq(UploadTaskDetail::getIdentifier, identifier));
                uploadTaskMapper.update(null, new LambdaUpdateWrapper<UploadTask>()
                        .set(UploadTask::getUploadStatus, UploadFileStatusEnum.SUCCESS.getCode())
                        .eq(UploadTask::getIdentifier, identifier));

                // 生成缩略图
                try {
                    if (UFOPUtils.isImageFile(uploadFileResult.getExtendName())) {
                        BufferedImage src = uploadFileResult.getBufferedImage();
                        imageMapper.insert(new Image()
                                .setImageWidth(src.getWidth())
                                .setImageHeight(src.getHeight())
                                .setFileId(fileBean.getFileId()));
                    }
                }
                catch (Exception e) {
                    log.error("生成图片缩略图失败！", e);
                }

                // 解析音乐文件
                fileDealComp.parseMusicFile(uploadFileResult.getExtendName(), uploadFileResult
                        .getStorageType()
                        .getCode(), uploadFileResult.getFileUrl(), fileBean.getFileId());
            }
            // 2、未完成上传
            else if (UploadFileStatusEnum.UNCOMPLATE.equals(uploadFileResult.getStatus())) {
                // 插入上传任务详情
                uploadTaskDetailMapper.insert(new UploadTaskDetail()
                        .setFilePath(qiwenFile.getParent())
                        .setFilename(qiwenFile.getNameNotExtend())
                        .setChunkNumber(uploadFileDto.getChunkNumber())
                        .setChunkSize((int) uploadFileDto.getChunkSize())
                        .setRelativePath(uploadFileDto.getRelativePath())
                        .setTotalChunks(uploadFileDto.getTotalChunks())
                        .setTotalSize((int) uploadFileDto.getTotalSize())
                        .setIdentifier(identifier));
            }
            // 3、上传失败
            else if (UploadFileStatusEnum.FAIL.equals(uploadFileResult.getStatus())) {
                // 更新上传任务状态
                uploadTaskDetailMapper.delete(new LambdaQueryWrapper<UploadTaskDetail>().eq(UploadTaskDetail::getIdentifier, identifier));
                uploadTaskMapper.update(null, new LambdaUpdateWrapper<UploadTask>()
                        .set(UploadTask::getUploadStatus, UploadFileStatusEnum.FAIL.getCode())
                        .eq(UploadTask::getIdentifier, identifier));
            }
        }

    }

    private String formatChatset(String str) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }
        if (StandardCharsets.ISO_8859_1
                .newEncoder()
                .canEncode(str)) {
            byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
            return new String(bytes, Charset.forName("GBK"));
        }
        return str;
    }

    @Override
    public void downloadFile(HttpServletResponse httpServletResponse, DownloadFileDTO downloadFileDTO) {
        // 获取用户文件信息
        UserFile userFile = userFileMapper.selectById(downloadFileDTO.getUserFileId());
        // 1、判断是否是文件
        if (userFile.isFile()) {
            // 获取文件信息
            FileBean fileBean = fileMapper.selectById(userFile.getFileId());
            // 获取下载器
            Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
            if (Objects.isNull(downloader)) {
                log.error("下载失败，文件存储类型不支持下载，storageType:{}", fileBean.getStorageType());
                throw new DownloadException("下载失败");
            }
            // 下载文件
            DownloadFile downloadFile = new DownloadFile();
            downloadFile.setFileUrl(fileBean.getFileUrl());
            httpServletResponse.setContentLengthLong(fileBean.getFileSize());
            downloader.download(httpServletResponse, downloadFile);
        }
        // 2、判断是否是文件夹
        else {
            // 获取用户文件列表
            QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
            List<UserFile> userFileList = userFileMapper.selectUserFileByLikeRightFilePath(qiwenFile.getPath(), userFile.getUserId());
            List<String> userFileIds = userFileList
                    .stream()
                    .map(UserFile::getUserFileId)
                    .collect(Collectors.toList());
            // 下载用户文件列表
            downloadUserFileList(httpServletResponse, userFile.getFilePath(), userFile.getFileName(), userFileIds);
        }
    }

    @Override
    public void downloadUserFileList(HttpServletResponse httpServletResponse, String filePath, String fileName, List<String> userFileIds) {
        // 创建临时目录
        final String staticPath = UFOPUtils.getStaticPath();
        final String tempPath = staticPath
                .concat("temp")
                .concat(File.separator);
        final File tempDirFile = new File(tempPath);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }

        // 创建压缩文件
        try (final FileOutputStream f = new FileOutputStream(tempPath
                .concat(fileName)
                .concat(".zip"));
             final CheckedOutputStream cos = new CheckedOutputStream(f, new Adler32());
             final ZipOutputStream zos = new ZipOutputStream(cos);
             final BufferedOutputStream out = new BufferedOutputStream(zos)) {
            // 遍历用户文件列表
            for (String userFileId : userFileIds) {
                // 获取用户文件信息
                UserFile userFile = userFileMapper.selectById(userFileId);
                // 1、判断是否是文件
                if (userFile.isFile()) {
                    // 获取文件信息
                    FileBean fileBean = fileMapper.selectById(userFile.getFileId());
                    // 获取下载器
                    Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
                    if (Objects.isNull(downloader)) {
                        log.error("下载失败，文件存储类型不支持下载，storageType:{}", fileBean.getStorageType());
                        throw new UploadException("下载失败");
                    }
                    // 下载文件
                    DownloadFile downloadFile = new DownloadFile();
                    downloadFile.setFileUrl(fileBean.getFileUrl());
                    try (InputStream inputStream = downloader.getInputStream(downloadFile);
                         BufferedInputStream bis = new BufferedInputStream(inputStream)) {
                        // 创建压缩文件
                        QiwenFile qiwenFile = new QiwenFile(StrUtil.removePrefix(userFile.getFilePath(), filePath), userFile.getFileName() + "." + userFile.getExtendName(), false);
                        zos.putNextEntry(new ZipEntry(qiwenFile.getPath()));

                        // 读取文件
                        int i;
                        byte[] buffer = new byte[1024];
                        while ((i = bis.read(buffer)) != -1) {
                            out.write(buffer, 0, i);
                        }
                    }
                    catch (IOException e) {
                        log.error("下载过程中出现异常：{}", e.getMessage(), e);
                    }
                    finally {
                        try {
                            out.flush();
                        }
                        catch (IOException e) {
                            log.error("刷新输出流失败：{}", e.getMessage(), e);
                        }
                    }
                }
                // 2、否则为文件夹
                else {
                    // 创建压缩文件
                    QiwenFile qiwenFile = new QiwenFile(StrUtil.removePrefix(userFile.getFilePath(), filePath), userFile.getFileName(), true);
                    zos.putNextEntry(new ZipEntry(qiwenFile.getPath() + QiwenFile.separator));
                    zos.closeEntry();
                }
            }
        }
        catch (Exception e) {
            log.error("压缩过程中出现异常：{}", e.getMessage(), e);
        }

        // 下传压缩文件
        String zipPath = "";
        try {
            // 获取下载器
            Downloader downloader = ufopFactory.getDownloader(StorageTypeEnum.LOCAL.getCode());
            DownloadFile downloadFile = new DownloadFile();
            downloadFile.setFileUrl("temp"
                    .concat(File.separator)
                    .concat(fileName)
                    .concat(".zip"));
            File tempFile = new File(UFOPUtils
                    .getStaticPath()
                    .concat(downloadFile.getFileUrl()));
            httpServletResponse.setContentLengthLong(tempFile.length());
            // 下载文件
            downloader.download(httpServletResponse, downloadFile);
            zipPath = UFOPUtils
                    .getStaticPath()
                    .concat("temp")
                    .concat(File.separator)
                    .concat(fileName)
                    .concat(".zip");
        }
        catch (Exception e) {
            //org.apache.catalina.connector.ClientAbortException: java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            if (e
                    .getMessage()
                    .contains("ClientAbortException")) {
                //该异常忽略不做处理
            }
            else {
                log.error("下传zip文件出现异常：{}", e.getMessage());
            }
        }
        finally {
            // 无论压缩文件下载成功与否都要删除
            File file = new File(zipPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public void previewFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO) {
        // 获取用户文件信息
        UserFile userFile = userFileMapper.selectById(previewDTO.getUserFileId());
        // 获取文件信息
        FileBean fileBean = fileMapper.selectById(userFile.getFileId());
        // 获取预览器
        Previewer previewer = ufopFactory.getPreviewer(fileBean.getStorageType());
        if (Objects.isNull(previewer)) {
            log.error("预览失败，文件存储类型不支持预览，storageType:{}", fileBean.getStorageType());
            throw new UploadException("预览失败");
        }
        PreviewFile previewFile = new PreviewFile();
        previewFile.setFileUrl(fileBean.getFileUrl());
        try {
            if ("true".equals(previewDTO.getIsMin())) {
                // 获取缩略图预览器
                previewer.imageThumbnailPreview(httpServletResponse, previewFile);
            }
            else {
                previewer.imageOriginalPreview(httpServletResponse, previewFile);
            }
        }
        catch (Exception e) {
            //org.apache.catalina.connector.ClientAbortException: java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            if (e
                    .getMessage()
                    .contains("ClientAbortException")) {
                //该异常忽略不做处理
            }
            else {
                log.error("预览文件出现异常：{}", e.getMessage());
            }

        }

    }

    @Override
    public void previewPictureFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO) {
        byte[] bytesUrl = Base64
                .getDecoder()
                .decode(previewDTO.getUrl());
        PictureFile pictureFile = new PictureFile();
        pictureFile.setFileUrl(new String(bytesUrl));
        pictureFile = pictureFileMapper.selectOne(new QueryWrapper<>(pictureFile));
        Previewer previewer = ufopFactory.getPreviewer(pictureFile.getStorageType());
        if (Objects.isNull(previewer)) {
            log.error("预览失败，文件存储类型不支持预览，storageType:{}", pictureFile.getStorageType());
            throw new UploadException("预览失败");
        }
        PreviewFile previewFile = new PreviewFile();
        previewFile.setFileUrl(pictureFile.getFileUrl());
        //        previewFile.setFileSize(pictureFile.getFileSize());
        try {

            String mime = MimeUtils.getMime(pictureFile.getExtendName());
            httpServletResponse.setHeader("Content-Type", mime);

            String fileName = pictureFile.getFileName() + "." + pictureFile.getExtendName();
            try {
                fileName = new String(fileName.getBytes("utf-8"), "ISO-8859-1");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            httpServletResponse.addHeader("Content-Disposition", "fileName=" + fileName);// 设置文件名

            previewer.imageOriginalPreview(httpServletResponse, previewFile);
        }
        catch (Exception e) {
            //org.apache.catalina.connector.ClientAbortException: java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            if (e
                    .getMessage()
                    .contains("ClientAbortException")) {
                //该异常忽略不做处理
            }
            else {
                log.error("预览文件出现异常：{}", e.getMessage());
            }

        }
    }

    @Override
    public void deleteFile(FileBean fileBean) {
        Deleter deleter = null;

        deleter = ufopFactory.getDeleter(fileBean.getStorageType());
        DeleteFile deleteFile = new DeleteFile();
        deleteFile.setFileUrl(fileBean.getFileUrl());
        deleter.delete(deleteFile);
    }

    @Override
    public Long selectStorageSizeByUserId(String userId) {
        return userFileMapper.selectStorageSizeByUserId(userId);
    }
}
