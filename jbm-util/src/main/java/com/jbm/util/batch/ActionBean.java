package com.jbm.util.batch;

import lombok.Data;

import java.util.Date;

/**
 * 操作对象
 *
 * @param <T> 通用类型
 * @author wesley
 */
@Data
public class ActionBean<T> {
    // 操作类型
    private final ActionType actionType;
    // 当前数量
    private final Integer currQuantity;
    // 日期
    private final Date date;
    // 对象
    private T obj;

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
