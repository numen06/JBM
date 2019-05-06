package com.jbm.framework.masterdata.usage.bean;

import java.io.Serializable;

/**
 * 存在类型code的接口
 *
 * @param <ID>
 * @param <CODE>
 * @author wesley
 */
public interface CodePrimaryKey<ID extends Serializable, CODE extends Serializable> extends PrimaryKey<ID> {
    /**
     * 表示自定义主键
     *
     * @return
     */
    CODE getCode();

    /**
     * 设置自定义主键
     *
     * @param code
     */
    void setCode(CODE code);
}
