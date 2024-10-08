package com.qiwenshare.ufop.operation.upload.domain;

import com.qiwenshare.ufop.constant.StorageTypeEnum;
import com.qiwenshare.ufop.constant.UploadFileStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

@Data
@Accessors(chain = true)
public class UploadFileResult
{
    private String fileName;
    private String extendName;
    private long fileSize;
    private String fileUrl;
    private String identifier;
    private StorageTypeEnum storageType;
    private UploadFileStatusEnum status;
    private BufferedImage bufferedImage;
}
