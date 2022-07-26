package com.jbm.framework.metadata.bean;

import com.jbm.framework.metadata.enumerate.ResultEnum;

import java.io.Serializable;
import java.util.UUID;

/**
 * 通讯之间返回值的封装类
 *
 * @author Wesley
 */
public class ResultBean implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String key;
    private Integer flag;
    private String[] messages;
    private Object result;

    public ResultBean() {
        super();
    }

    public ResultBean(String key, Integer flag) {
        super();
        this.key = key;
        this.flag = flag;
    }

    public ResultBean(String key, Integer flag, String[] messages, Object result) {
        super();
        this.key = key;
        this.flag = flag;
        this.messages = messages;
        this.result = result;
    }

    public static ResultBean createResultBean() {
        return new ResultBean(UUID.randomUUID().toString(), ResultEnum.none.getResult());
    }

    public static ResultBean createResultBean(ResultEnum flag) {
        return new ResultBean(UUID.randomUUID().toString(), flag.getResult(), null, null);
    }

    public static ResultBean createResultBean(ResultEnum flag, Object result) {
        return new ResultBean(UUID.randomUUID().toString(), flag.getResult(), null, result);
    }

    public static ResultBean createResultBean(ResultEnum flag, Object result, String... messages) {
        return new ResultBean(UUID.randomUUID().toString(), flag.getResult(), messages, result);
    }

    public static ResultBean createResultBean(Throwable throwable) {
        return new ResultBean(UUID.randomUUID().toString(), ResultEnum.error.getResult(), new String[]{throwable.getMessage()}, null);
    }

    public static boolean isSuccess(ResultBean result) {
        return compareToResultEnum(ResultEnum.success, result);
    }

    public static boolean compareToResultEnum(ResultEnum resultEnum, ResultBean result) {
        return resultEnum.getResult().equals(result.getFlag());
    }

    public static boolean compareToResultEnum(ResultEnum resultEnum, Integer result) {
        return resultEnum.getResult().equals(result);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getFlag() {
        return flag;
    }

    public void setFlag(Integer flag) {
        this.flag = flag;
    }

    public String[] getMessages() {
        return messages;
    }

    public void setMessages(String[] messages) {
        this.messages = messages;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
