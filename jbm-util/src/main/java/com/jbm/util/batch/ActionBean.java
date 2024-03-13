package com.jbm.util.batch;

import lombok.Data;

import java.util.Date;
import java.util.concurrent.BlockingQueue;

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
    private final BlockingQueue<T> blockingQueue;
    // 日期
    private final Date date;
    // 对象
    private T obj;

    /**
     * 构造函数
     *
     * @param actionType   操作类型
     * @param blockingQueue 当前数量
     * @param date         日期
     */
    public ActionBean(ActionType actionType, BlockingQueue<T> blockingQueue, Date date) {
        this.actionType = actionType;
        this.blockingQueue = blockingQueue;
        this.date = date;
    }

    /**
     * 构造函数
     *
     * @param actionType   操作类型
     * @param blockingQueue 当前数量
     * @param date         日期
     * @param obj          对象
     */
    public ActionBean(ActionType actionType, BlockingQueue<T> blockingQueue, Date date, T obj) {
        this.actionType = actionType;
        this.blockingQueue = blockingQueue;
        this.date = date;
        this.obj = obj;
    }
}
