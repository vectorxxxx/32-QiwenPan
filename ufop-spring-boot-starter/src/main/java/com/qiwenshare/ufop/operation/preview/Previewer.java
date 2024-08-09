package com.qiwenshare.ufop.operation.preview;

import cn.hutool.http.HttpUtil;
import com.qiwenshare.common.operation.ImageOperation;
import com.qiwenshare.ufop.domain.ThumbImage;
import com.qiwenshare.ufop.exception.operation.PreviewException;
import com.qiwenshare.ufop.operation.preview.domain.PreviewFile;
import com.qiwenshare.ufop.util.CharsetUtils;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@Data
public abstract class Previewer
{

    public ThumbImage thumbImage;

    protected abstract InputStream getInputStream(PreviewFile previewFile);

    public void imageThumbnailPreview(HttpServletResponse httpServletResponse, PreviewFile previewFile) {
        String fileUrl = previewFile.getFileUrl();

        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            String[] arr = fileUrl
                    .replace("http://", "")
                    .replace("https://", "")
                    .split("/");
            String name = arr[0];
            String icoUrl = findIco(fileUrl);

            File cacheFile = UFOPUtils.getCacheFile(UFOPUtils.getUploadFileUrl(name, "ico"));
            if (cacheFile.exists()) {
                FileInputStream fis = null;
                OutputStream outputStream = null;
                try {
                    fis = new FileInputStream(cacheFile);
                    outputStream = httpServletResponse.getOutputStream();
                    IOUtils.copy(fis, outputStream);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    IOUtils.closeQuietly(fis);
                    IOUtils.closeQuietly(outputStream);
                }
            }
            else {
                URL url = null;
                InputStream in = null;
                OutputStream outputStream = null;
                try {
                    url = new URL(icoUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    InputStream inputstream = connection.getInputStream();
                    try {
                        outputStream = httpServletResponse.getOutputStream();
                        in = ImageOperation.thumbnailsImageForScale(inputstream, cacheFile, 50);
                        IOUtils.copy(in, outputStream);

                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(inputstream);
                        IOUtils.closeQuietly(outputStream);
                        if (previewFile.getOssClient() != null) {
                            previewFile
                                    .getOssClient()
                                    .shutdown();
                        }
                    }
                }
                catch (MalformedURLException e) {

                    log.error("MalformedURLException, url is {}", icoUrl);
                    throw new RuntimeException(e);
                }
                catch (IOException e) {
                    log.error("IOException, url is {}", icoUrl);
                    throw new RuntimeException(e);
                }
            }

            return;
        }

        boolean isVideo = UFOPUtils.isVideoFile(FilenameUtils.getExtension(fileUrl));
        String thumbnailImgUrl = previewFile.getFileUrl();
        if (isVideo) {
            thumbnailImgUrl = fileUrl.replace("." + FilenameUtils.getExtension(fileUrl), ".jpg");
        }

        File cacheFile = UFOPUtils.getCacheFile(thumbnailImgUrl);

        if (cacheFile.exists()) {
            FileInputStream fis = null;
            OutputStream outputStream = null;
            try {
                fis = new FileInputStream(cacheFile);
                outputStream = httpServletResponse.getOutputStream();
                IOUtils.copy(fis, outputStream);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                IOUtils.closeQuietly(fis);
                IOUtils.closeQuietly(outputStream);
            }

        }
        else {
            OutputStream outputStream = null;
            InputStream in = null;
            InputStream inputstream = null;
            try {
                inputstream = getInputStream(previewFile);
            }
            catch (PreviewException previewException) {
                log.error(previewException.getMessage());
                return;
            }

            try {
                outputStream = httpServletResponse.getOutputStream();
                in = ImageOperation.thumbnailsImageForScale(inputstream, cacheFile, 50);
                IOUtils.copy(in, outputStream);

            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(inputstream);
                IOUtils.closeQuietly(outputStream);
                if (previewFile.getOssClient() != null) {
                    previewFile
                            .getOssClient()
                            .shutdown();
                }
            }

        }
    }

    public void imageOriginalPreview(HttpServletResponse httpServletResponse, PreviewFile previewFile) {

        InputStream inputStream = null;

        OutputStream outputStream = null;

        try {
            inputStream = getInputStream(previewFile);
            outputStream = httpServletResponse.getOutputStream();
            byte[] bytes = IOUtils.toByteArray(inputStream);
            bytes = CharsetUtils.convertTxtCharsetToUTF8(bytes, FilenameUtils.getExtension(previewFile.getFileUrl()));
            outputStream.write(bytes);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
            if (previewFile.getOssClient() != null) {
                previewFile
                        .getOssClient()
                        .shutdown();
            }
        }
    }

    /**
     * 根据网页URL获取favicon图标URL
     *
     * @param navUrl 网页的URL地址
     * @return favicon图标文件的URL
     */
    private static String findIco(String navUrl) {
        // 发送HTTP GET请求获取网页内容
        String body = HttpUtil
                .createGet(navUrl)
                .execute()
                .toString();
        // 从网页内容中提取favicon图标的相关信息
        String str = body.split("favicon\\d{0,3}.ico")[0];
        // 尝试找到以https://开头的URL
        int http = str.indexOf("https://", str.length() - 100);
        // 如果未找到，再尝试以http://开头的URL
        if (http == -1) {
            http = str.indexOf("http://", str.length() - 100);
        }
        // 如果仍未找到，则认为网页中没有指定favicon图标的绝对路径，需要进行拼接
        if (http == -1) {
            // 获取网址中用于拼接favicon.ico的部分
            int i = navUrl.indexOf("/", 8);//获取网址 拼接 favicon.ico
            if (i > 0) {
                navUrl = navUrl.substring(0, i);
            }
        }
        // 如果找到了以http或https开头的URL，则直接使用该URL
        else {
            navUrl = str.substring(http);
        }
        // 拼接favicon图标文件的URL并返回
        return navUrl + "/favicon.ico";
    }

}
