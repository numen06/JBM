package com.jbm.framework.metadata.bean;

import com.jbm.framework.metadata.enumerate.MessageEnum;
import com.jbm.framework.metadata.enumerate.ResultEnum;
import lombok.Data;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 后台返回给前台的封装类
 *
 * @author Wesley
 */
@Data
public class ResultFormFegin<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 返回状态码
     */
    private Integer status = 200;

    /**
     * 返回值
     */
    private T result;

    /**
     * 时间戳
     */
    private Date timestamp;

    /**
     * 消息提示
     */
    private String message;

    /**
     * 是否成功
     */
    private Boolean success;


}
