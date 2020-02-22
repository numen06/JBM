package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author wesley
 */
@Data
@MappedSuperclass
public class MasterDataTreeEntity extends MasterDataIdEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 层级
     */
    private Integer level;

    //	@Transient
    @TableField(exist = false)
    private Boolean leaf;
}
