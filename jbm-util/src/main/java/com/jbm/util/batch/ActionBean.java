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
    private final Date submitTime;

    private final Date lastActionTime;
    // 对象
    private T obj;

    /**
     * 构造函数
     *
     * @param actionType   操作类型
     * @param currQuantity 当前数量
     * @param submitTime   日期
     */
    public ActionBean(ActionType actionType, Integer currQuantity, Date submitTime, Date lastActionTime) {
        this.actionType = actionType;
        this.currQuantity = currQuantity;
        this.submitTime = submitTime;
        this.lastActionTime = lastActionTime;
    }

    /**
     * 构造函数
     *
     * @param actionType   操作类型
     * @param currQuantity 当前数量
     * @param submitTime   日期
     * @param obj          对象
     */
    public ActionBean(ActionType actionType, Integer currQuantity, Date submitTime, Date lastActionTime, T obj) {
        this.actionType = actionType;
        this.currQuantity = currQuantity;
        this.submitTime = submitTime;
        this.lastActionTime = lastActionTime;
        this.obj = obj;
    }
}
