package com.jbm.framework.metadata.bean;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import com.jbm.util.throwing.ThrowingCallback;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.ValidationException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wesley
 */
@ApiModel(value = "JSON响应结果")
@Data
@NoArgsConstructor
public class ResultBody<T> implements Serializable {
    public static final Integer SUCCESS = HttpStatus.HTTP_OK;
    public static final Integer FAIL = HttpStatus.HTTP_INTERNAL_ERROR;
    private static final long serialVersionUID = 1L;

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
    @ApiModelProperty(value = "错误信息")
    private String exception;

    public static <D> ResultBody<D> ok() {
        return new ResultBody<D>().code(ErrorCode.OK.getCode()).msg(ErrorCode.OK.getMessage());
    }

    public static <D> ResultBody<D> ok(D data) {
        ResultBody<D> resultBody = new ResultBody<>();
        return resultBody.code(ErrorCode.OK.getCode()).msg(ErrorCode.OK.getMessage()).data(data);
    }

    public static <D> ResultBody<D> failed() {
        return new ResultBody<D>().code(ErrorCode.FAIL.getCode()).msg(ErrorCode.FAIL.getMessage());
    }

    public static <D> ResultBody<D> failed(D data) {
        return new ResultBody<D>().data(data).code(ErrorCode.FAIL.getCode()).msg(ErrorCode.FAIL.getMessage());
    }

    public static <T> ResultBody<T> success() {
        return ResultBody.ok();
    }

    public static <T> ResultBody<T> error() {
        return ResultBody.failed();
    }

    public static <T> ResultBody<T> success(T data, String msg) {
        return ResultBody.ok(data).msg(msg);
    }

    public static <T> ResultBody<T> success(String msg) {
        return new ResultBody<T>().code(ErrorCode.OK.getCode()).msg(msg).data(null);
    }

    public static <T> ResultBody<T> error(T data, String msg) {
        return new ResultBody<T>().code(ErrorCode.ERROR.getCode()).msg(msg).data(data);
    }

    public static <T> ResultBody<T> error(String e) {
        return new ResultBody<T>().code(ErrorCode.FAIL.getCode()).msg(e);
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

    public static <T> ResultBody<T> error(Exception e) {
        if (e instanceof ServiceException) {
            return ResultBody.error(e.getMessage());
        }
        if (e instanceof ValidateException) {
            return ResultBody.error(e.getMessage());
        }
        if (e instanceof ValidationException) {
            return ResultBody.error(e.getMessage());
        }
        return (ResultBody<T>) ResultBody.failed().exception(e);
    }

    public static <T> ResultBody<T> error(T data, String msg, Exception e) {
        if (e instanceof ServiceException) {
            return ResultBody.failed(data).msg(e.getMessage());
        }
        return ResultBody.failed(data).msg(msg).exception(e);
    }

    public static <T> ResultBody<T> callback(ThrowingCallback<T> supplier) {
        try {
            T res = supplier.get();
            return ResultBody.ok(res);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    public static <T> ResultBody<T> callback(String msg, ThrowingCallback<T> supplier) {
        try {
            T res = supplier.get();
            return ResultBody.ok(res).msg(msg);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    public static <T> ResultBody<T> call(Runnable runnable) {
        try {
            runnable.run();
            return ResultBody.ok(null);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    public static <T> ResultBody<T> call(String msg, Runnable runnable) {
        try {
            runnable.run();
            return ResultBody.success(msg);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }


    public ResultBody<T> code(int code) {
        this.code = code;
        this.success = this.code == ErrorCode.OK.getCode();
        return this;
    }

    public ResultBody<T> msg(String message) {
        this.message = message;
        return this;
    }

    public ResultBody<T> data(T data) {
        this.result = data;
        return this;
    }

    public ResultBody<T> path(String path) {
        this.path = path;
        return this;
    }

    public ResultBody<T> httpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    public ResultBody<T> put(String key, Object value) {
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

    public ResultBody<T> exception(Throwable e) {
        if (e == null) {
            exception = null;
        } else {
            this.exception = StrUtil.isNotEmpty(e.getMessage()) ? e.getMessage() : e.getClass().getName();
        }
        return this;
    }

    public Map<String, Object> toMap() {
        return BeanUtil.beanToMap(this, false, true);
    }

    public <R> R action(Function<? super T, ? super R> function) {
        return (R) function.apply(this.result);
    }

    public void action(Consumer<? super T> consumer) {
        consumer.accept(this.result);
    }


}
