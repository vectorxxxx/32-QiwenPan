package com.qiwenshare.ufop.operation.upload.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * UploadFile 类用于管理文件切片上传的过程
 * 它包含了切片上传过程中所需的各种参数和信息
 */
@Data
@Accessors(chain = true)
public class UploadFile
{

    // 第n个切片，用于标识当前切片的序号
    private int chunkNumber;

    // 当前切片的大小，单位为字节
    private long chunkSize;

    // 文件总切片数，表示文件被分割成了多少个切片
    private int totalChunks;

    // 上传的唯一标识符，用于关联不同的切片属于同一个文件
    private String identifier;

    // 所有切片的总大小，即原文件的大小，单位为字节
    private long totalSize;

    // 当前切片的实际大小，可能与chunkSize不同，尤其在最后一个切片时
    private long currentChunkSize;
}

