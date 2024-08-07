package com.qiwenshare.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 统一结果返回
 *
 * @param <T> 参数泛型
 */
@Data
@Schema(name = "统一结果返回",
        required = true)
public class RestResult<T>
{
    @Schema(description = "请求是否成功",
            example = "true")
    private Boolean success = true;

    @Schema(description = "返回码",
            example = "000000")
    private Integer code = 000000;

    @Schema(description = "返回信息",
            example = "成功")
    private String message;

    @Schema(description = "返回数据")
    private T data;

    @Schema(description = "返回数据列表")
    private List<T> dataList;

    @Schema(description = "总数")
    private long total;

    // 通用返回成功
    public static <T> RestResult<T> success() {
        RestResult<T> r = new RestResult<>();
        r.setSuccess(ResultCodeEnum.SUCCESS.getSuccess());
        r.setCode(ResultCodeEnum.SUCCESS.getCode());
        r.setMessage(ResultCodeEnum.SUCCESS.getMessage());
        return r;
    }

    // 通用返回失败，未知错误
    public static <T> RestResult<T> fail() {
        RestResult<T> r = new RestResult<>();
        r.setSuccess(ResultCodeEnum.UNKNOWN_ERROR.getSuccess());
        r.setCode(ResultCodeEnum.UNKNOWN_ERROR.getCode());
        r.setMessage(ResultCodeEnum.UNKNOWN_ERROR.getMessage());
        return r;
    }

    // 设置结果，形参为结果枚举
    public static <T> RestResult<T> setResult(ResultCodeEnum result) {
        RestResult<T> r = new RestResult<>();
        r.setSuccess(result.getSuccess());
        r.setCode(result.getCode());
        r.setMessage(result.getMessage());
        return r;
    }

    // 自定义返回数据
    public RestResult<T> data(T param) {
        this.setData(param);
        return this;
    }

    public RestResult<T> dataList(List<T> param, long total) {
        this.setDataList(param);
        this.setTotal(total);
        return this;
    }

    // 自定义状态信息
    public RestResult<T> message(String message) {
        this.setMessage(message);
        return this;
    }

    // 自定义状态码
    public RestResult<T> code(Integer code) {
        this.setCode(code);
        return this;
    }

    // 自定义返回结果
    public RestResult<T> success(Boolean success) {
        this.setSuccess(success);
        return this;
    }

}
