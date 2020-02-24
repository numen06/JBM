package com.jbm.framework.metadata.bean;


import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Maps;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author admin
 */
@ApiModel(value = "响应结果")
@Data
public class ResultBody<T> implements Serializable {
    /**
     * 响应编码
     */
    @ApiModelProperty(value = "响应编码:0-请求处理成功")
    private int code = 0;
    /**
     * 提示消息
     */
    @ApiModelProperty(value = "提示消息")
    private String message;

    /**
     * 请求路径
     */
    @ApiModelProperty(value = "请求路径")
    private String path;

    /**
     * 响应数据
     */
    @ApiModelProperty(value = "响应数据")
    private T data;

    /**
     * http状态码
     */
    private int httpStatus;

    /**
     * 附加数据
     */
    @ApiModelProperty(value = "附加数据")
    private Map<String, Object> extra;

    /**
     * 响应时间
     */
    @ApiModelProperty(value = "响应时间")
    private long timestamp = System.currentTimeMillis();

    public ResultBody() {
        super();
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public T getData() {
        return data;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public long getTimestamp() {
        return timestamp;
    }

//    @JSONField(serialize = false, deserialize = false)
    public int getHttpStatus() {
        return httpStatus;
    }

//    @JSONField(serialize = false, deserialize = false)
//    public boolean isOk() {
//        return this.code == ErrorCode.OK.getCode();
//    }


    public static ResultBody ok() {
        return new ResultBody().code(ErrorCode.OK.getCode()).msg(ErrorCode.OK.getMessage());
    }

    public static ResultBody failed() {
        return new ResultBody().code(ErrorCode.FAIL.getCode()).msg(ErrorCode.FAIL.getMessage());
    }

    public ResultBody code(int code) {
        this.code = code;
        return this;
    }

    public ResultBody msg(String message) {
        this.message = i18n(ErrorCode.getResultEnum(this.code).getMessage(), message);
        return this;
    }

    public ResultBody data(T data) {
        this.data = data;
        return this;
    }

    public ResultBody path(String path) {
        this.path = path;
        return this;
    }

    public ResultBody httpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    public ResultBody put(String key, Object value) {
        if (this.extra == null) {
            this.extra = Maps.newHashMap();
        }
        this.extra.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "ResultBody{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                ", data=" + data +
                ", httpStatus=" + httpStatus +
                ", extra=" + extra +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * 错误信息配置
     */
    @JSONField(serialize = false, deserialize = false)
    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("error");

    /**
     * 提示信息国际化
     *
     * @param message
     * @param defaultMessage
     * @return
     */
    @JSONField(serialize = false, deserialize = false)
    private static String i18n(String message, String defaultMessage) {
        return resourceBundle.containsKey(message) ? resourceBundle.getString(message) : defaultMessage;
    }

    @ApiModelProperty(value = "错误信息")
    private String exception;

    @ApiModelProperty(value = "是否成功")
    private Boolean getSuccess() {
        return this.code == ErrorCode.OK.getCode();
    }


    public static <T> ResultForm<T> success(T data, String msg) {
        return ResultForm.ok().result(data).msg(msg);
    }

    public static <T> ResultForm<T> error(T data, String msg) {
        return ResultForm.failed().result(data).msg(msg);
    }

    public static <T> ResultForm<T> error(Exception e) {
        return ResultForm.failed().exception(e);
    }

    public static <T> ResultForm<T> error(T data, String msg, Exception e) {
        return ResultForm.failed().result(data).msg(msg).exception(e);
    }


}
