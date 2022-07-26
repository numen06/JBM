package com.jbm.framework.metadata.usage.bean;

import java.io.Serializable;

/**
 * 标示主键
 *
 * @param <ID>
 * @author Wesley
 */
public interface PrimaryKey<ID extends Serializable> extends Serializable {

    ID getId();

    void setId(ID id);
}
