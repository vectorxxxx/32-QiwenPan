package com.qiwenshare.ufop.operation.upload.product;

import com.qiwenshare.ufop.constant.StorageTypeEnum;
import com.qiwenshare.ufop.constant.UploadFileStatusEnum;
import com.qiwenshare.ufop.exception.operation.UploadException;
import com.qiwenshare.ufop.operation.upload.Uploader;
import com.qiwenshare.ufop.operation.upload.domain.UploadFile;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import com.qiwenshare.ufop.operation.upload.request.QiwenMultipartFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class LocalStorageUploader extends Uploader
{

    public static Map<String, String> FILE_URL_MAP = new HashMap<>();

    /**
     * 执行上传流程
     *
     * @param qiwenMultipartFile qiwenMultipartFile
     * @param uploadFile         上传文件
     * @return {@link UploadFileResult }
     */
    @Override
    protected UploadFileResult doUploadFlow(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile) {
        UploadFileResult uploadFileResult = new UploadFileResult();
        try {
            // 获取文件路径
            String fileUrl = UFOPUtils.getUploadFileUrl(uploadFile.getIdentifier(), qiwenMultipartFile.getExtendName());
            if (StringUtils.isNotEmpty(FILE_URL_MAP.get(uploadFile.getIdentifier()))) {
                fileUrl = FILE_URL_MAP.get(uploadFile.getIdentifier());
            }
            else {
                FILE_URL_MAP.put(uploadFile.getIdentifier(), fileUrl);
            }
            String tempFileUrl = fileUrl + "_tmp";
            String confFileUrl = fileUrl.replace("." + qiwenMultipartFile.getExtendName(), ".conf");

            File file = new File(UFOPUtils.getStaticPath() + fileUrl);
            File tempFile = new File(UFOPUtils.getStaticPath() + tempFileUrl);
            File confFile = new File(UFOPUtils.getStaticPath() + confFileUrl);

            // 第一步 打开将要写入的文件
            try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
                // 第二步 打开通道
                FileChannel fileChannel = raf.getChannel();
                // 第三步 计算偏移量
                long position = (uploadFile.getChunkNumber() - 1) * uploadFile.getChunkSize();
                // 第四步 获取分片数据
                byte[] fileData = qiwenMultipartFile.getUploadBytes();
                // 第五步 写入数据
                fileChannel.position(position);
                fileChannel.write(ByteBuffer.wrap(fileData));
                fileChannel.force(true);
                fileChannel.close();
            }

            // 封装返回结果
            uploadFileResult
                    .setIdentifier(uploadFile.getIdentifier())
                    .setFileUrl(fileUrl)
                    .setFileName(qiwenMultipartFile.getFileName())
                    .setExtendName(qiwenMultipartFile.getExtendName())
                    .setFileSize(uploadFile.getTotalSize())
                    .setStorageType(StorageTypeEnum.LOCAL);
            if (uploadFile.getTotalChunks() == 1) {
                uploadFileResult.setFileSize(qiwenMultipartFile.getSize());
            }

            // 判断是否完成文件的传输并进行校验与重命名
            boolean isComplete = checkUploadStatus(uploadFile, confFile);
            if (isComplete) {
                tempFile.renameTo(file);
                FILE_URL_MAP.remove(uploadFile.getIdentifier());

                if (UFOPUtils.isImageFile(uploadFileResult.getExtendName())) {
                    try (InputStream is = new FileInputStream(UFOPUtils.getLocalSaveFile(fileUrl))) {
                        BufferedImage src = ImageIO.read(is);
                        uploadFileResult.setBufferedImage(src);
                    }
                    catch (IOException e) {
                        log.error("图片读取失败：{}", e.getMessage(), e);
                    }
                }

                uploadFileResult.setStatus(UploadFileStatusEnum.SUCCESS);
            }
            else {
                uploadFileResult.setStatus(UploadFileStatusEnum.UNCOMPLATE);
            }
        }
        catch (IOException e) {
            throw new UploadException(e);
        }

        return uploadFileResult;
    }

    @Override
    public void cancelUpload(UploadFile uploadFile) {
        String fileUrl = FILE_URL_MAP.get(uploadFile.getIdentifier());
        String tempFileUrl = fileUrl + "_tmp";
        String confFileUrl = fileUrl.replace("." + FilenameUtils.getExtension(fileUrl), ".conf");
        File tempFile = new File(tempFileUrl);
        if (tempFile.exists()) {
            tempFile.delete();
        }
        File confFile = new File(confFileUrl);
        if (confFile.exists()) {
            confFile.delete();
        }
    }

    @Override
    protected void doUploadFileChunk(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile) {

    }

    @Override
    protected UploadFileResult organizationalResults(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile) {
        return null;
    }

}
