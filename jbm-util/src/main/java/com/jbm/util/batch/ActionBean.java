package com.jbm.util.batch;

import lombok.Data;

import java.util.Date;

/**
 * 操作对象
 *
 * @author wesley
 * @param <T> 通用类型
 */
@Data
public class ActionBean<T> {

    private final ActionType actionType;    // 操作类型
    private final Integer currQuantity;    // 当前数量
    private final Date date;    // 日期
    private T obj;    // 对象

    /**
     * 构造函数
     *
     * @param actionType   操作类型
     * @param currQuantity 当前数量
     * @param date         日期
     */
    public ActionBean(ActionType actionType, Integer currQuantity, Date date) {
        this.actionType = actionType;
        this.currQuantity = currQuantity;
        this.date = date;
    }

    /**
     * 构造函数
     *
     * @param actionType   操作类型
     * @param currQuantity 当前数量
     * @param date         日期
     * @param obj          对象
     */
    public ActionBean(ActionType actionType, Integer currQuantity, Date date, T obj) {
        this.actionType = actionType;
        this.currQuantity = currQuantity;
        this.date = date;
        this.obj = obj;
    }
}
