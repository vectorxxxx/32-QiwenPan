package com.qiwenshare.common.operation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VideoOperation
{
    public static byte[] addBytes(byte[] data1, byte[] data2) {

        byte[] data3 = new byte[data1.length + data2.length];

        System.arraycopy(data1, 0, data3, 0, data1.length);

        System.arraycopy(data2, 0, data3, data1.length, data2.length);

        return data3;

    }
    //    public static void main(String[] args) {
    //        File file = new File("E:\\export\\upload\\20220604\\1f54c2a2173812ca1be5e126c8650131.mp4");
    //
    //        try {
    //            FileInputStream inputStream = new FileInputStream(file);
    //            int catchLong  = 1024* 1024 * 15;
    //            System.out.println(inputStream.available());
    //            byte[] bytes = new byte[catchLong];
    //            inputStream.read(bytes);
    //            System.out.println(inputStream.available());
    //            byte[] bytes1 = new byte[catchLong];
    //            inputStream.skip(inputStream.available() - catchLong);
    ////            System.out.println(bytes1.length - inputStream.available() - 102400);
    //            inputStream.read(bytes1);
    //            byte[] byteAll = addBytes(bytes, bytes1);
    //
    ////            InputStream in = new ByteArrayInputStream(byteAll);
    ////            System.out.println(in.available());
    //            FileUtils.writeByteArrayToFile(new File("E:\\export\\upload\\20220604\\1f54c2a2173812ca1be5e126c8650131_min.mp4"), byteAll);
    ////            thumbnailsImage(inputStream, new File("E:\\export\\upload\\20220604\\1f54c2a2173812ca1be5e126c8650131_min.jpg"), 150, 150);
    //        } catch (IOException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }
    //
    //    public static InputStream thumbnailsImage(InputStream inputStream, File outFile, int width, int height) throws IOException {
    //
    //        if (inputStream == null) {
    //            throw new QiwenException(999999, "Get the video preview. The input stream is null.");
    //        }
    //        try {
    //            FFmpegFrameGrabber ff = new FFmpegFrameGrabber(inputStream);
    //            ff.start();
    //            // 视频总帧数
    //            int videoLength = ff.getLengthInFrames();
    //            Frame f  = null;
    //            int i = 0;
    //            while (i < videoLength) {
    //                // 过滤前20帧,因为前20帧可能是全黑的
    //                // 这里看需求，也可以直接根据帧数取图片
    //                f = ff.grabFrame();
    //                if (i > 20 && f.image != null) {
    //                    break;
    //                }
    //                i++;
    //            }
    //            int owidth = f.imageWidth;
    //            int oheight = f.imageHeight;
    //            // 对截取的帧进行等比例缩放
    ////            int width = 800;
    //            height = (int) (((double) width / owidth) * oheight);
    //            Java2DFrameConverter converter = new Java2DFrameConverter();
    //            BufferedImage fecthedImage = converter.getBufferedImage(f);
    //            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    //            bi.getGraphics().drawImage(fecthedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
    //                    0, 0, null);
    //
    //            File saveDir = outFile.getParentFile().getAbsoluteFile();
    //            if (!saveDir.exists()) {
    //                saveDir.mkdirs();
    //            }
    //
    //            ImageIO.write(bi, "jpg", outFile);
    //            ff.stop();
    //
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        } catch (Exception e) {
    //            String errorMessage = e.getMessage();
    //            if (errorMessage.contains("AWTError")) {
    //                log.info(e.getMessage());
    //            }
    //            log.error(e.getMessage());
    //        }
    //        return new FileInputStream(outFile);
    //
    //
    //    }
}
