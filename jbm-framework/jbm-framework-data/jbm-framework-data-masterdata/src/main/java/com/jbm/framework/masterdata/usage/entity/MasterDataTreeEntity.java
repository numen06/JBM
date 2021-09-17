package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.MappedSuperclass;

/**
 * @author wesley
 */
@Data
@TableName
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public abstract class MasterDataTreeEntity extends MasterDataIdEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * 父ID
     */
    @ApiModelProperty("树状结构父ID")
    private Long parentId;

    /**
     * 层级
     */
    @ApiModelProperty("树状结构层级")
    private Integer level;


    //	@Transient
    @TableField(exist = false)
    @ApiModelProperty("树状结构是否叶子节点")
    private Boolean leaf;
}
