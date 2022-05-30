package com.jbm.framework.metadata.bean;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.http.HttpStatus;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wesley
 */
@ApiModel(value = "JSON响应结果")
@Data
@NoArgsConstructor
public class ResultBody<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final Integer SUCCESS = HttpStatus.HTTP_INTERNAL_ERROR;

    public static final Integer FAIL = HttpStatus.HTTP_OK;


    /**
     * 响应编码
     */
    @ApiModelProperty(value = "响应编码:0-请求处理成功")
    private Integer code = ErrorCode.OK.getCode();
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
    private T result;

    /**
     * http状态码
     */
    @ApiModelProperty(value = "http状态码")
    private Integer httpStatus;

    /**
     * 附加数据
     */
    @ApiModelProperty(value = "附加数据")
    private Map<String, Object> extra;

    /**
     * 响应时间
     */
    @ApiModelProperty(value = "响应时间")
    private Date timestamp = DateTime.now();

    @ApiModelProperty(value = "是否成功")
    private Boolean success = true;

//    public ResultBody() {
//        super();
//    }


    public static ResultBody ok() {
        return new ResultBody().code(ErrorCode.OK.getCode()).msg(ErrorCode.OK.getMessage());
    }

    public static ResultBody ok(Object data) {
        return new ResultBody().code(ErrorCode.OK.getCode()).msg(ErrorCode.OK.getMessage()).data(data);
    }

    public static ResultBody failed() {
        return new ResultBody().code(ErrorCode.FAIL.getCode()).msg(ErrorCode.FAIL.getMessage());
    }

    public ResultBody code(int code) {
        this.code = code;
        this.success = this.code == ErrorCode.OK.getCode();
        return this;
    }

    public ResultBody msg(String message) {
        this.message = message;
        return this;
    }

    public ResultBody data(T data) {
        this.result = data;
        return this;
    }

    public ResultBody path(String path) {
        this.path = path;
        return this;
    }

    public ResultBody httpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    public ResultBody put(String key, Object value) {
        if (this.extra == null) {
            this.extra = new HashMap<>();
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
                ", result=" + result +
                ", httpStatus=" + httpStatus +
                ", extra=" + extra +
                ", timestamp=" + timestamp +
                '}';
    }

//    /**
//     * 错误信息配置
//     */
//    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("error");
//
//    /**
//     * 提示信息国际化
//     *
//     * @param message
//     * @param defaultMessage
//     * @return
//     */
//    private static String i18n(String message, String defaultMessage) {
//        return resourceBundle.containsKey(message) ? resourceBundle.getString(message) : defaultMessage;
//    }

    @ApiModelProperty(value = "错误信息")
    private String exception;


    public ResultBody exception(Throwable e) {
        if (e == null) {
            exception = null;
        } else {
            this.exception = e.getMessage();
        }
        return this;
    }

    public static <T> ResultBody<T> success() {
        return ResultBody.ok();
    }

    public static <T> ResultBody<T> error() {
        return ResultBody.failed();
    }

    public static <T> ResultBody<T> success(T data, String msg) {
        return ResultBody.ok().data(data).msg(msg);
    }

    public static <T> ResultBody<T> success(String msg) {
        return ResultBody.ok().data(null).msg(msg);
    }

    public static <T> ResultBody<T> error(T data, String msg) {
        return ResultBody.failed().data(data).msg(msg);
    }

    public static <T> ResultBody<T> error(String e) {
        return ResultBody.failed().msg(e);
    }

    public static <T> ResultBody<T> error(Exception e) {
        if (e instanceof ServiceException) {
            return ResultBody.failed().data(null).msg(e.getMessage()).exception(e);
        }
        return ResultBody.failed().exception(e);
    }

    public static <T> ResultBody<T> error(T data, String msg, Exception e) {
        if (e instanceof ServiceException) {
            return ResultBody.failed().data(data).msg(e.getMessage()).exception(e);
        }
        return ResultBody.failed().data(data).msg(msg).exception(e);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = BeanUtil.beanToMap(this, false, true);
        return result;
    }


}
