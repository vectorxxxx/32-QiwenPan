package com.qiwenshare.ufop.operation.preview.product;

import com.qiwenshare.ufop.domain.ThumbImage;
import com.qiwenshare.ufop.exception.operation.PreviewException;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.operation.preview.domain.PreviewFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
public class LocalStoragePreviewer extends Previewer
{

    public LocalStoragePreviewer() {

    }

    public LocalStoragePreviewer(ThumbImage thumbImage) {
        setThumbImage(thumbImage);
    }

    /**
     * 获取输入流
     *
     * @param previewFile 预览文件
     * @return {@link InputStream }
     */
    @Override
    protected InputStream getInputStream(PreviewFile previewFile) {
        // 设置文件路径
        File file = UFOPUtils.getLocalSaveFile(previewFile.getFileUrl());
        if (!file.exists()) {
            throw new PreviewException("[UFOP] Failed to get the file stream because the file path does not exist! The file path is: " + file.getAbsolutePath());
        }
        byte[] bytes;
        try (InputStream inputStream = new FileInputStream(file)) {
            bytes = IOUtils.toByteArray(inputStream);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ByteArrayInputStream(bytes);
    }
}
