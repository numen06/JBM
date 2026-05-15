package com.jbm.framework.masterdata.usage.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * 技术的实体
 *
 * @author wesley
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 7148367690448503947L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    public BaseEntity() {
        super();
    }

    public BaseEntity(Long id) {
        super();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
