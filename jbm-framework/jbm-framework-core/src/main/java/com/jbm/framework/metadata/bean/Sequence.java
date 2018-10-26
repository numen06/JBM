package com.jbm.framework.metadata.bean;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * User: wesley.zhang
 * Date: 14-7-30
 * Time: 下午5:20
 */
public class Sequence implements Serializable {

    private static final long serialVersionUID = 758247529357293475L;

    private Long id;

    private String tableName;

    private Long latestSequence;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getLatestSequence() {
        return latestSequence;
    }

    public void setLatestSequence(Long latestSequence) {
        this.latestSequence = latestSequence;
    }
}
