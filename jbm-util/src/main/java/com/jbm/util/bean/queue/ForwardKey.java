package com.jbm.util.bean.queue;

import java.io.Serializable;

/**
 * 转发的主键
 *
 * @param <K>
 * @author Numen
 */
public class ForwardKey<K> implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // public static final ForwardKey<Integer> NORMAL_KEY = new
    // ForwardKey<Integer>(ForwardType.NORMAL, null);
    //
    // public static final ForwardKey<Integer> RECOVER_KEY = new
    // ForwardKey<Integer>(ForwardType.RECOVER, null);

    private final ForwardType forwardType;
    private K key;

    public ForwardKey(K key) {
        super();
        this.forwardType = ForwardType.FORWARD;
        this.key = key;
    }

    public ForwardKey(ForwardType forwardType, K key) {
        super();
        this.forwardType = forwardType;
        this.key = key;
    }

    public ForwardType getForwardType() {
        return forwardType;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

}
