package com.qiwenshare.ufop.operation.download.product;

import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

@Slf4j
@Component
public class LocalStorageDownloader extends Downloader
{

    /**
     * 获取输入流
     *
     * @param downloadFile 下载文件
     * @return {@link InputStream }
     */
    @Override
    public InputStream getInputStream(DownloadFile downloadFile) {
        // 设置文件路径
        File file = new File(UFOPUtils
                .getStaticPath()
                .concat(downloadFile.getFileUrl()));

        byte[] bytes = new byte[0];
        if (downloadFile.getRange() != null) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                randomAccessFile.seek(downloadFile
                        .getRange()
                        .getStart());
                bytes = new byte[downloadFile
                        .getRange()
                        .getLength()];
                randomAccessFile.read(bytes);
            }
            catch (IOException e) {
                log.error("读取文件失败：{}", e.getMessage(), e);
            }
        }
        else {
            try (InputStream inputStream = new FileInputStream(file)) {
                bytes = IOUtils.toByteArray(inputStream);
            }
            catch (IOException e) {
                log.error("读取文件失败：{}", e.getMessage(), e);
            }
        }

        return new ByteArrayInputStream(bytes);
    }
}
