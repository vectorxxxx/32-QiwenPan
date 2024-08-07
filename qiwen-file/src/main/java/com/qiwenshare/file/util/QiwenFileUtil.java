package com.qiwenshare.file.util;

import cn.hutool.core.util.IdUtil;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.constant.FileDirEnum;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.io.QiwenFile;

public class QiwenFileUtil
{

    public static UserFile getQiwenDir(String userId, String filePath, String fileName) {
        final String currentTime = DateUtil.getCurrentTime();
        return new UserFile()
                .setUserFileId(IdUtil.getSnowflakeNextIdStr())
                .setUserId(userId)
                .setFileId(null)
                .setFileName(fileName)
                .setFilePath(QiwenFile.formatPath(filePath))
                .setExtendName(null)
                .setIsDir(FileDirEnum.DIR.getType())
                .setCreateUserId(SessionUtil.getUserId())
                .setCreateTime(currentTime)
                .setUploadTime(currentTime)
                .setDeleteFlag(FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                .setDeleteBatchNum(null);
    }

    public static UserFile getQiwenFile(String userId, String fileId, String filePath, String fileName, String extendName) {
        return new UserFile()
                .setUserFileId(IdUtil.getSnowflakeNextIdStr())
                .setUserId(userId)
                .setFileId(fileId)
                .setFileName(fileName)
                .setFilePath(QiwenFile.formatPath(filePath))
                .setExtendName(extendName)
                .setIsDir(0)
                .setUploadTime(DateUtil.getCurrentTime())
                .setCreateTime(DateUtil.getCurrentTime())
                .setCreateUserId(SessionUtil.getUserId())
                .setDeleteFlag(FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                .setDeleteBatchNum(null);
    }

    public static UserFile searchQiwenFileParam(UserFile userFile) {
        return new UserFile()
                .setFilePath(QiwenFile.formatPath(userFile.getFilePath()))
                .setFileName(userFile.getFileName())
                .setExtendName(userFile.getExtendName())
                .setDeleteFlag(FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                .setUserId(userFile.getUserId())
                .setIsDir(0);
    }

    public static String formatLikePath(String filePath) {
        return filePath
                .replace("'", "\\'")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

}
