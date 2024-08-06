package com.qiwenshare.common.exception;

import com.qiwenshare.common.result.ResultCodeEnum;
import lombok.Data;

/**
 * 自定义全局异常类
 */
@Data
public class QiwenException extends RuntimeException
{
    private Integer code;

    public QiwenException(String message) {
        super(message);
        this.code = ResultCodeEnum.UNKNOWN_ERROR.getCode();
    }

    public QiwenException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public QiwenException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    @Override
    public String toString() {
        return "QiwenException{" + "code=" + code + ", message=" + this.getMessage() + '}';
    }
}
