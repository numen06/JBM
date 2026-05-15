package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 基础类模型
 *
 * @author wesley
 */
@Data
@TableName
public abstract class MasterDataEntity implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键ID")
    private Long id;

    /**
     * 业务编码
     */
    @ApiModelProperty("业务编码")
    private String code;

    /**
     * 应用ID
     */
    @ApiModelProperty("应用ID")
    private Long appId;

    /**
     * 树状结构父ID
     */
    @ApiModelProperty("树状结构父ID")
    private Long parentId;

    /**
     * 树状结构层级
     */
    @ApiModelProperty("树状结构层级")
    private Integer level;

    /**
     * 树状结构的路径
     */
    @ApiModelProperty("树状结构的路径")
    private String leafPath;

    /**
     * 树状结构是否叶子节点
     */
    @TableField(exist = false)
    @ApiModelProperty("树状结构是否叶子节点")
    private Boolean leaf;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private Date updateTime;

}
