package com.qiwenshare.ufop.operation.delete;

import com.qiwenshare.ufop.operation.delete.domain.DeleteFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

@Slf4j
public abstract class Deleter
{
    public abstract void delete(DeleteFile deleteFile);

    protected void deleteCacheFile(DeleteFile deleteFile) {
        // 如果不是图片文件，则不删除本地缓存文件
        if (!UFOPUtils.isImageFile(FilenameUtils.getExtension(deleteFile.getFileUrl()))) {
            return;
        }

        // 获取本地缓存文件
        File cacheFile = UFOPUtils.getCacheFile(deleteFile.getFileUrl());
        if (cacheFile.exists()) {
            boolean result = cacheFile.delete();
            if (!result) {
                log.error("删除本地缓存文件失败！");
            }
        }
    }
}
