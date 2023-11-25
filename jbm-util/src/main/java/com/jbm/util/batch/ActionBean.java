package com.jbm.util.batch;

import lombok.Data;

import java.util.Date;

@Data
public class ActionBean<T> {

    private final ActionType actionType;
    private final Integer currQuantity;
    private final Date date;
    private   T obj;

    public ActionBean(ActionType actionType, Integer currQuantity, Date date) {
        this.actionType = actionType;
        this.currQuantity = currQuantity;
        this.date = date;
    }
    public ActionBean(ActionType actionType, Integer currQuantity, Date date, T obj) {
        this.actionType = actionType;
        this.currQuantity = currQuantity;
        this.date = date;
        this.obj = obj;
    }
}
