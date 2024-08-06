package com.qiwenshare.common.operation;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class ImageOperation
{
    /**
     * 左旋
     *
     * @param inFile  源文件
     * @param outFile 目的文件
     * @param angle   角度
     * @throws IOException io异常
     */
    public static void leftTotation(File inFile, File outFile, int angle) throws IOException {
        Thumbnails
                .of(inFile)
                .scale(1)
                .outputQuality(1)
                .rotate(-angle)
                .toFile(outFile);
    }

    /**
     * 右旋
     *
     * @param inFile  源文件
     * @param outFile 目的文件
     * @param angle   角度
     * @throws IOException io异常
     */
    public static void rightTotation(File inFile, File outFile, int angle) throws IOException {
        Thumbnails
                .of(inFile)
                .scale(1)
                .outputQuality(1)
                .rotate(angle)
                .toFile(outFile);
    }

    /**
     * 压缩
     *
     * @param inFile  源文件
     * @param outFile 目的文件
     * @param width   图像宽
     * @param height  图像高
     * @throws IOException io异常
     */
    public static void thumbnailsImage(File inFile, File outFile, int width, int height) throws IOException {

        Thumbnails
                .of(inFile)
                .size(width, height)
                .toFile(outFile);

    }

    public static InputStream thumbnailsImage(InputStream inputStream, File outFile, int width, int height) throws IOException {
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        ByteArrayOutputStream baos = cloneInputStream(inputStream);
        // 打开两个新的输入流
        InputStream inputStream1 = new ByteArrayInputStream(baos.toByteArray());
        InputStream inputStream2 = new ByteArrayInputStream(baos.toByteArray());
        BufferedImage bufferedImage = ImageIO.read(inputStream1);
        if (bufferedImage == null) {
            return inputStream2;
        }
        int oriHeight = bufferedImage.getHeight();
        int oriWidth = bufferedImage.getWidth();

        if (oriHeight <= height || oriWidth <= width) {
            ImageIO.write(bufferedImage, FilenameUtils.getExtension(outFile.getName()), outFile);
        }
        else {
            if (oriHeight < oriWidth) {
                Thumbnails
                        .of(bufferedImage)
                        .outputQuality(1)
                        .scale(1)
                        .sourceRegion(Positions.CENTER, oriHeight, oriHeight)
                        .toFile(outFile);
            }
            else {
                Thumbnails
                        .of(bufferedImage)
                        .outputQuality(1)
                        .scale(1)
                        .sourceRegion(Positions.CENTER, oriWidth, oriWidth)
                        .toFile(outFile);
            }
            Thumbnails
                    .of(ImageIO.read(outFile))
                    .outputQuality(0.9)
                    .size(width, height)
                    .toFile(outFile);

        }
        return new FileInputStream(outFile);

    }

    public static InputStream thumbnailsImageForScale(InputStream inputStream, File outFile, long desFileSize) throws IOException {

        byte[] imageBytes = IOUtils.toByteArray(inputStream);
        if (imageBytes == null || imageBytes.length <= 0 || imageBytes.length < desFileSize * 1024) {
            FileUtils.writeByteArrayToFile(outFile, imageBytes);
            return new FileInputStream(outFile);
        }
        long srcSize = imageBytes.length;
        //double accuracy = getAccuracy(srcSize / 1024);
        double accuracy = 0.4;
        try {
            while (imageBytes.length > desFileSize * 1024) {
                ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(imageBytes.length);
                Thumbnails
                        .of(is)
                        .scale(accuracy)
                        .outputQuality(accuracy)
                        .toOutputStream(outputStream);
                imageBytes = outputStream.toByteArray();
            }
        }
        catch (Exception e) {
            log.error("【图片压缩】msg=图片压缩失败!", e);
        }
        FileUtils.writeByteArrayToFile(outFile, imageBytes);
        return new FileInputStream(outFile);
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    public static String getFileExtendName(String fileName) {
        if (fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private static ByteArrayOutputStream cloneInputStream(InputStream input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            return baos;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
