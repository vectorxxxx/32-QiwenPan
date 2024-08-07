package com.qiwenshare.common.operation;

import lombok.extern.slf4j.Slf4j;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
public class FileOperation
{
    private static Executor executor = Executors.newFixedThreadPool(20);

    /**
     * 创建文件
     *
     * @param fileUrl 文件路径
     * @return 新文件
     */
    public static File newFile(String fileUrl) {
        File file = new File(fileUrl);
        return file;
    }

    /**
     * 删除文件
     *
     * @param file 文件
     * @return 是否删除成功
     */
    public static boolean deleteFile(File file) {
        if (Objects.isNull(file)) {
            return false;
        }

        if (!file.exists()) {
            return false;

        }

        if (file.isFile()) {
            return file.delete();
        }
        else {
            for (File newfile : file.listFiles()) {
                deleteFile(newfile);
            }
        }
        return file.delete();
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件路径
     * @return 删除是否成功
     */
    public static boolean deleteFile(String fileUrl) {
        File file = newFile(fileUrl);
        return deleteFile(file);
    }

    /**
     * 得到文件大小
     *
     * @param fileUrl 文件路径
     * @return 文件大小
     */
    public static long getFileSize(String fileUrl) {
        File file = newFile(fileUrl);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

    /**
     * 得到文件大小
     *
     * @param file 文件
     * @return 文件大小
     */
    public static long getFileSize(File file) {
        if (Objects.isNull(file)) {
            return 0;
        }
        return file.length();
    }

    /**
     * 创建目录
     *
     * @param file 文件
     * @return 是否创建成功
     */
    public static boolean mkdir(File file) {
        if (Objects.isNull(file)) {
            return false;
        }

        if (file.exists()) {
            return true;
        }

        return file.mkdirs();
    }

    /**
     * 创建目录
     *
     * @param fileUrl 文件路径
     * @return 是否创建成功
     */
    public static boolean mkdir(String fileUrl) {
        if (StringUtils.isEmpty(fileUrl)) {
            return false;
        }
        File file = newFile(fileUrl);
        if (file.exists()) {
            return true;
        }

        return file.mkdirs();
    }

    /**
     * 拷贝文件
     *
     * @param fileInputStream  文件输入流
     * @param fileOutputStream 文件输出流
     * @throws IOException io异常
     */
    public static void copyFile(FileInputStream fileInputStream, FileOutputStream fileOutputStream) throws IOException {
        try {
            byte[] buf = new byte[4096];  //8k的缓冲区

            int len = fileInputStream.read(buf); //数据在buf中，len代表向buf中放了多少个字节的数据，-1代表读不到
            while (len != -1) {

                fileOutputStream.write(buf, 0, len); //读多少个字节，写多少个字节

                len = fileInputStream.read(buf);
            }

        }
        finally {
            if (Objects.nonNull(fileInputStream)) {
                try {
                    fileInputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (Objects.nonNull(fileOutputStream)) {
                try {
                    fileOutputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    /**
     * 拷贝文件
     *
     * @param src  源文件
     * @param dest 目的文件
     * @throws IOException io异常
     */
    public static void copyFile(File src, File dest) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dest);

        copyFile(in, out);

    }

    /**
     * 拷贝文件
     *
     * @param srcUrl  源路径
     * @param destUrl 目的路径
     * @throws IOException io异常
     */
    public static void copyFile(String srcUrl, String destUrl) throws IOException {
        if (StringUtils.isEmpty(srcUrl) || StringUtils.isEmpty(destUrl)) {
            return;
        }
        File srcFile = newFile(srcUrl);
        File descFile = newFile(destUrl);
        copyFile(srcFile, descFile);
    }

    /**
     * 文件解压缩
     *
     * @param sourceFile  源文件
     * @param destDirPath 目的文件路径
     * @return 文件列表
     * @throws Exception 异常
     */
    public static List<String> unzip(File sourceFile, String destDirPath) throws Exception {

        IInArchive archive;
        RandomAccessFile randomAccessFile;
        // 第一个参数是需要解压的压缩包路径，第二个参数参考JdkAPI文档的RandomAccessFile
        //r代表以只读的方式打开文本，也就意味着不能用write来操作文件
        randomAccessFile = new RandomAccessFile(sourceFile, "r");
        archive = SevenZip.openInArchive(null, // null - autodetect
                new RandomAccessFileInStream(randomAccessFile));
        int[] in = new int[archive.getNumberOfItems()];
        for (int i = 0; i < in.length; i++) {
            in[i] = i;
        }
        archive.extract(in, false, new ExtractCallback(archive, destDirPath));
        File destFile = new File(destDirPath);

        Collection<File> files = FileUtils.listFiles(destFile, new IOFileFilter()
        {
            @Override
            public boolean accept(File file) {
                return true;
            }

            @Override
            public boolean accept(File file, String s) {
                return true;
            }
        }, new IOFileFilter()
        {
            @Override
            public boolean accept(File file) {
                return true;
            }

            @Override
            public boolean accept(File file, String s) {
                return true;
            }
        });
        Set<String> set = new HashSet<>();
        files.forEach(o -> {
            String path = o
                    .getAbsolutePath()
                    .replace(destFile.getAbsolutePath(), "")
                    .replace("\\", "/");
            if (StringUtils.isNotEmpty(path)) {
                set.add(path);
            }
        });

        List<String> res = new ArrayList<>(set);
        return res;
    }

    //    public static void main(String[] args) {
    //        try {
    //            unrar5(new File("C:\\Users\\马超\\Downloads\\FineAgent.rar"), "E:\\export\\rar5test");
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //        System.out.println("结束");
    //    }

    /**
     * 保存数据
     *
     * @param filePath 文件路径
     * @param fileName 文件名
     * @param data     数据
     */
    public static void saveDataToFile(String filePath, String fileName, String data) {
        BufferedWriter writer = null;

        File dir = new File(filePath);

        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }

        File file = new File(filePath + fileName);

        //如果文件不存在，则新建一个
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        //写入
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));
            writer.write(data);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (Objects.nonNull(writer)) {
                    writer.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("文件写入成功！");
    }

}
