package com.qiwenshare.file.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author VectorX
 * @version V1.0
 * @description
 * @date 2024-08-07 10:39:47
 */
@AllArgsConstructor
@Getter
public enum FileDirEnum
{
    FILE(0, "文件"),
    DIR(1, "目录");

    private Integer type;
    private String desc;
}
