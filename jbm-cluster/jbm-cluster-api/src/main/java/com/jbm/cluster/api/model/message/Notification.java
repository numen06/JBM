package com.jbm.cluster.api.model.message;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 03:52
 **/
@Data
public abstract class Notification implements Serializable {

    public String getNotifType() {
        return this.getClass().getName();
    }
}
