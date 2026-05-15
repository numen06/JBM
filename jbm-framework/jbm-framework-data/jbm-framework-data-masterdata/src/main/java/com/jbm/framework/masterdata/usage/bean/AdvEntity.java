package com.jbm.framework.masterdata.usage.bean;

import com.baomidou.mybatisplus.annotation.TableField;

/**
 * 封装ID和CODE两大唯一键的高级实体
 *
 * @author wesley
 */
public class AdvEntity extends BaseEntity implements CodePrimaryKey<Long, String> {
    private static final long serialVersionUID = 4915439801688748572L;

    @TableField("code")
    private String code;

    public AdvEntity() {
        super();
    }

    public AdvEntity(String code) {
        super();
        this.code = code;
    }

    public AdvEntity(Long id, String code) {
        super(id);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

}
