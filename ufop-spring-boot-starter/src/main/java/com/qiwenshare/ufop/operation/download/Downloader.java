package com.qiwenshare.ufop.operation.download;

import com.aliyun.oss.OSS;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author VectorX
 * @version 1.0.0
 * @description 下载器
 * @date 2024/08/09
 */
@Slf4j
public abstract class Downloader
{

    /**
     * 下载
     *
     * @param httpServletResponse httpServletResponse
     * @param downloadFile        下载文件
     */
    public void download(HttpServletResponse httpServletResponse, DownloadFile downloadFile) {
        // 尝试打开输入流和输出流，以复制下载文件到响应对象中
        try (InputStream inputStream = getInputStream(downloadFile);
             OutputStream outputStream = httpServletResponse.getOutputStream()) {
            IOUtils.copyLarge(inputStream, outputStream);
        }
        // 捕获可能发生的IO异常，并记录错误信息
        catch (IOException e) {
            log.error("下载文件失败：{}", e.getMessage(), e);
        }
        // 确保关闭OSS客户端，避免资源泄露
        finally {
            OSS ossClient = downloadFile.getOssClient();
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public abstract InputStream getInputStream(DownloadFile downloadFile);
}
