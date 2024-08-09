package com.qiwenshare.ufop.operation.upload;

import com.qiwenshare.ufop.exception.operation.UploadException;
import com.qiwenshare.ufop.operation.upload.domain.UploadFile;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import com.qiwenshare.ufop.operation.upload.request.QiwenMultipartFile;
import com.qiwenshare.ufop.util.RedisUtil;
import com.qiwenshare.ufop.util.concurrent.locks.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author VectorX
 * @version 1.0.0
 * @description 上传器
 * @date 2024/08/09
 */
@Slf4j
@Component
public abstract class Uploader
{
    @Resource
    RedisLock redisLock;
    @Resource
    RedisUtil redisUtil;

    /**
     * 普通上传
     *
     * @param httpServletRequest http的request请求
     * @return 文件列表
     */
    public List<UploadFileResult> upload(HttpServletRequest httpServletRequest) {
        UploadFile uploadFile = new UploadFile()
                .setChunkNumber(1)
                .setChunkSize(0)
                .setTotalChunks(1)
                .setIdentifier(UUID
                        .randomUUID()
                        .toString());
        return upload(httpServletRequest, uploadFile);
    }

    /**
     * 分片上传
     *
     * @param httpServletRequest http的request请求
     * @param uploadFile         分片上传参数
     * @return 文件列表
     */
    public List<UploadFileResult> upload(HttpServletRequest httpServletRequest, UploadFile uploadFile) {
        // 创建文件结果集
        List<UploadFileResult> uploadFileResultList = new ArrayList<>();

        // 获取request
        StandardMultipartHttpServletRequest request = (StandardMultipartHttpServletRequest) httpServletRequest;

        // 判断是否是文件请求
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            throw new UploadException("未包含文件上传域");
        }

        try {
            // 获取请求中的文件名迭代器
            Iterator<String> iter = request.getFileNames();
            // 遍历文件名，处理每个文件
            while (iter.hasNext()) {
                // 根据当前文件名获取文件列表
                List<MultipartFile> multipartFileList = request.getFiles(iter.next());
                // 遍历文件列表，上传每个文件
                for (MultipartFile multipartFile : multipartFileList) {
                    // 将文件包装为QiwenMultipartFile，以便支持Qiwen上传逻辑
                    QiwenMultipartFile qiwenMultipartFile = new QiwenMultipartFile(multipartFile);
                    // 执行上传流程并获取结果
                    UploadFileResult uploadFileResult = doUploadFlow(qiwenMultipartFile, uploadFile);
                    // 将上传结果添加到结果列表中
                    uploadFileResultList.add(uploadFileResult);
                }
            }
        }
        // 捕获上传过程中可能出现的异常，并抛出自定义的上传异常
        catch (Exception e) {
            throw new UploadException(e);
        }

        return uploadFileResultList;
    }

    protected UploadFileResult doUploadFlow(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile) {
        // 尝试上传文件并处理相关结果
        UploadFileResult uploadFileResult;
        try {
            // 矫正上传文件的相关信息，确保文件信息准确无误
            rectifier(qiwenMultipartFile, uploadFile);
            // 组织上传文件的结果，返回上传相关信息
            uploadFileResult = organizationalResults(qiwenMultipartFile, uploadFile);
        } catch (Exception e) {
            // 捕获异常并转换为上传异常，以便于错误处理
            throw new UploadException(e);
        }

        return uploadFileResult;
    }

    /**
     * 取消上传
     *
     * @param uploadFile 分片上传参数
     */
    public abstract void cancelUpload(UploadFile uploadFile);

    protected abstract void doUploadFileChunk(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile) throws IOException;

    protected abstract UploadFileResult organizationalResults(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile);

    /**
     * 整流器
     *
     * @param qiwenMultipartFile qiwenMultipartFile
     * @param uploadFile         上传文件
     */
    private void rectifier(QiwenMultipartFile qiwenMultipartFile, UploadFile uploadFile) {
        String key = "QiwenUploader:Identifier:"
                .concat(uploadFile.getIdentifier())
                .concat(":lock");
        String current_upload_chunk_number = "QiwenUploader:Identifier:"
                .concat(uploadFile.getIdentifier())
                .concat(":current_upload_chunk_number");

        // 使用Redis锁以确保线程安全地处理文件上传
        redisLock.lock(key);
        try {
            // 初始化上传切片编号，如果不存在则创建并设置为1
            if (StringUtils.isEmpty(redisUtil.getObject(current_upload_chunk_number))) {
                redisUtil.set(current_upload_chunk_number, "1", 1000 * 60 * 60);
            }
            // 获取当前上传切片编号
            int currentUploadChunkNumber = Integer.parseInt(redisUtil.getObject(current_upload_chunk_number));

            // 检查上传文件的切片编号是否与当前编号匹配
            if (uploadFile.getChunkNumber() != currentUploadChunkNumber) {
                // 如果不匹配，释放锁并短暂等待，以处理可能的并发情况
                redisLock.unlock(key);
                Thread.sleep(100);
                // 尝试重新获取锁，如果无法获取，则继续尝试，直到成功或超过设定的等待时间
                while (redisLock.tryLock(key, 300, TimeUnit.SECONDS)) {
                    // 重新获取当前上传切片编号
                    currentUploadChunkNumber = Integer.parseInt(redisUtil.getObject(current_upload_chunk_number));

                    // 如果上传文件的切片编号不大于等于当前切片编号，则退出循环
                    if (uploadFile.getChunkNumber() <= currentUploadChunkNumber) {
                        break;
                    }
                    // 如果切片编号差异过大，记录错误日志并抛出异常
                    if (Math.abs(currentUploadChunkNumber - uploadFile.getChunkNumber()) > 2) {
                        log.error("传入的切片数据异常，当前应上传切片为第{}块，传入的为第{}块。", currentUploadChunkNumber, uploadFile.getChunkNumber());
                        throw new UploadException("传入的切片数据异常");
                    }
                    // 释放锁，以便其他线程可以尝试上传
                    redisLock.unlock(key);
                }
            }

            // 记录文件上传的日志信息
            log.info("文件名{},正在上传第{}块, 共{}块>>>>>>>>>>", qiwenMultipartFile
                    .getMultipartFile()
                    .getOriginalFilename(), uploadFile.getChunkNumber(), uploadFile.getTotalChunks());
            // 如果上传文件的切片编号与当前切片编号匹配，则继续上传
            if (uploadFile.getChunkNumber() == currentUploadChunkNumber) {
                // 执行具体的文件切片上传操作
                doUploadFileChunk(qiwenMultipartFile, uploadFile);
                // 记录文件切片上传成功的日志信息
                log.info("文件名{},第{}块上传成功", qiwenMultipartFile
                        .getMultipartFile()
                        .getOriginalFilename(), uploadFile.getChunkNumber());
                // 更新已上传切片的计数
                this.redisUtil.getIncr("QiwenUploader:Identifier:"
                        .concat(uploadFile.getIdentifier())
                        .concat(":current_upload_chunk_number"));
            }
        }
        catch (Exception e) {
            // 如果上传过程中发生异常，记录错误日志并设置当前上传切片编号为当前值，然后抛出自定义异常
            log.error("第{}块上传失败，自动重试", uploadFile.getChunkNumber());
            redisUtil.set("QiwenUploader:Identifier:"
                    .concat(uploadFile.getIdentifier())
                    .concat(":current_upload_chunk_number"), String.valueOf(uploadFile.getChunkNumber()), 1000 * 60 * 60);
            throw new UploadException("更新远程文件出错", e);
        }
        finally {
            // 最终确保释放Redis锁
            redisLock.unlock(key);
        }
    }

    public synchronized boolean checkUploadStatus(UploadFile param, File confFile) throws IOException {
        RandomAccessFile confAccessFile = new RandomAccessFile(confFile, "rw");
        try {
            //设置文件长度
            confAccessFile.setLength(param.getTotalChunks());
            //设置起始偏移量
            confAccessFile.seek(param.getChunkNumber() - 1);
            //将指定的一个字节写入文件中 127，
            confAccessFile.write(Byte.MAX_VALUE);

        }
        finally {
            IOUtils.closeQuietly(confAccessFile);
        }
        byte[] completeStatusList = FileUtils.readFileToByteArray(confFile);
        //创建conf文件文件长度为总分片数，每上传一个分块即向conf文件中写入一个127，那么没上传的位置就是默认的0,已上传的就是127
        for (byte b : completeStatusList) {
            if (b != Byte.MAX_VALUE) {
                return false;
            }
        }
        confFile.delete();
        return true;
    }

    public void writeByteDataToFile(byte[] fileData, File file, UploadFile uploadFile) {
        //第一步 打开将要写入的文件
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "rw");
            //第二步 打开通道
            FileChannel fileChannel = raf.getChannel();
            //第三步 计算偏移量
            long position = (uploadFile.getChunkNumber() - 1) * uploadFile.getChunkSize();
            //第四步 获取分片数据
            //            byte[] fileData = qiwenMultipartFile.getUploadBytes();
            //第五步 写入数据
            fileChannel.position(position);
            fileChannel.write(ByteBuffer.wrap(fileData));
            fileChannel.force(true);
            fileChannel.close();
            raf.close();
        }
        catch (IOException e) {
            throw new UploadException(e);
        }

    }

}
