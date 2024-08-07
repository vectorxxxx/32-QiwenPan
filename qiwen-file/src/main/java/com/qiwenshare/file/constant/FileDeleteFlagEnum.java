package com.qiwenshare.file.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author VectorX
 * @version 1.0.0
 * @description File del Flag 枚举
 * @date 2024/07/23
 * @see Enum
 */
@AllArgsConstructor
@Getter
public enum FileDeleteFlagEnum
{

    NOT_DELETED(0),
    DELETED(1);

    private Integer deleteFlag;
}
