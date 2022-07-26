package com.jbm.test.disruptor.bean;

import java.io.Serializable;

/**
 * 处理对象
 *
 * @author wesley.zhang
 */
public class TradeTransaction implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 4240094876064541520L;
    private String id; // 交易ID
    private double price;// 交易金额
    private int ruku = 0;

    private int vas = 0;

    private int jsm = 0;

    public TradeTransaction() {
        super();
    }

    public TradeTransaction(String id, double price) {
        super();
        this.id = id;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getRuku() {
        return ruku;
    }

    public void setRuku(int ruku) {
        this.ruku = ruku;
    }

    public int getVas() {
        return vas;
    }

    public void setVas(int vas) {
        this.vas = vas;
    }

    public int getJsm() {
        return jsm;
    }

    public void setJsm(int jsm) {
        this.jsm = jsm;
    }

}
