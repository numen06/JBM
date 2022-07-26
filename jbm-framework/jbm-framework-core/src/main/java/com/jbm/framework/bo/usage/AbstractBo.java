package com.jbm.framework.bo.usage;

import java.util.UUID;

/**
 * 的bo类
 *
 * @author wesley
 */
public abstract class AbstractBo<P extends IBizParam, A extends IBizAction> implements IBo<P, A> {

    private static final long serialVersionUID = -958417582335733371L;

    private String sn = UUID.randomUUID().toString();

    private P param;

    private A action;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public P getParam() {
        return param;
    }

    public void setParam(P param) {
        this.param = param;
    }

    public A getAction() {
        return action;
    }

    public void setAction(A action) {
        this.action = action;
    }

}
