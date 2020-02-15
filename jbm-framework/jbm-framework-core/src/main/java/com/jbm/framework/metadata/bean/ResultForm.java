package com.jbm.framework.metadata.bean;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.enumerate.MessageEnum;
import com.jbm.framework.metadata.enumerate.ResultEnum;

/**
 * 后台返回给前台的封装类
 *
 * @author Wesley
 */
@Deprecated
public class ResultForm implements Serializable {

    private static final long serialVersionUID = 1L;

    public ResultForm() {
        this(200);
    }

    public ResultForm(int status) {
        super();
        this.status = status;
    }

    public ResultForm(Integer status, Object result, List<ResultMessage> messages) {
        super();
        this.status = status;
        this.result = result;
        this.messages = messages;
    }

    public ResultForm(Integer status, Integer errorCode, Object result, List<ResultMessage> messages) {
        super();
        this.status = status;
        this.errorCode = errorCode;
        this.result = result;
        this.messages = messages;
    }

    /**
     * 创建一个正确的返回值
     *
     * @param result  返回值
     * @param message 消息
     * @param args    参数
     * @return
     */
    public static ResultForm success(Object result, String message, String... args) {
        return createResultForm(ResultEnum.success, result, MessageEnum.success,
                MessageFormat.format(message, message));
    }

    /**
     * 创建一个错误的返回值
     *
     * @param result  返回值
     * @param message 消息
     * @param args    参数
     * @return
     */
    public static ResultForm error(Object result, String message, String... args) {
        return createResultForm(ResultEnum.error, result, MessageEnum.error, MessageFormat.format(message, message));
    }

    public static ResultForm error(Object result, Integer errorCode, String message, String... args) {
        return createResultForm(ResultEnum.error, result, errorCode, MessageEnum.error,
                MessageFormat.format(message, message));
    }

    public static ResultForm error(Object result, String message, Exception e) {
        return error(result, 500, message, e);
    }

    public static ResultForm error(Exception e) {
        if (e instanceof ServiceException) {
            return error(null, 500, "", e);
        }
        return error(null, 500, "", e);
    }

    public static ResultForm error(Object result, Integer errorCode, String message, Exception e) {
        return createResultForm(ResultEnum.error, result, errorCode, MessageEnum.error, message, e.getMessage());
    }

    private static ResultForm createResultForm(ResultEnum status, Object result, MessageEnum level,
                                               String... messages) {
        return new ResultForm(status.getResult(), result, ResultMessage.createReslutMessages(level, messages));
    }

    private static ResultForm createResultForm(ResultEnum status, Object result, Integer errorCode, MessageEnum level,
                                               String... messages) {
        return new ResultForm(status.getResult(), errorCode, result,
                ResultMessage.createReslutMessages(level, messages));
    }

    /**
     * 返回状态码
     */
    private Integer status = 200;

    private Integer errorCode;

    /**
     * 返回值
     */
    private Object result;

    private Date timestamp = new Date();
    /**
     * 返回消息
     */
    private List<ResultMessage> messages = new ArrayList<ResultMessage>();

    public Object getResult() {
        return result;
    }

    public String getMessage() {
        StringBuffer ms = new StringBuffer();
        for (ResultMessage messgae : this.messages) {
            ms.append(messgae.getBody());
        }
        return ms.toString();
    }

    public void setMessage(String message) {
        this.messages.clear();
        messages.add(new ResultMessage(this.status, message));
    }

    public boolean isSuccess() {
        return this.status == ResultEnum.success.getResult();
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public Date getTimestamp() {
        return timestamp;
    }

}
