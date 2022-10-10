package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * 系统资源-菜单信息
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:21
 * @description:
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_menu")
@ApiModel(value = "系统菜单")
public class BaseMenu extends MasterDataEntity {
    private static final long serialVersionUID = -4414780909980518788L;
    /**
     * 菜单Id
     */
    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "菜单ID")
    private Long menuId;

    /**
     * 菜单编码
     */
    @ApiModelProperty(value = "菜单编码")
    private String menuCode;

    /**
     * 菜单名称
     */
    @ApiModelProperty(value = "菜单名称")
    private String menuName;

    /**
     * 图标
     */
    @ApiModelProperty(value = "菜单图标")
    private String icon;

    /**
     * 父级菜单
     */
    @ApiModelProperty(value = "父ID")
    private Long parentId;

    /**
     * 请求协议:/,http://,https://
     */
    @ApiModelProperty(value = "请求协议:/,http://,https://")
    private String scheme;

    /**
     * 请求路径
     */
    @ApiModelProperty(value = "请求路径")
    private String path;

    /**
     * 打开方式:_self窗口内,_blank新窗口
     */
    @ApiModelProperty(value = "打开方式:_self窗口内,_blank新窗口")
    private String target;

    /**
     * 优先级 越小越靠前
     */
    @ApiModelProperty(value = "优先级 越小越靠前")
    private Integer priority;

    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String menuDesc;

    /**
     * 状态:0-无效 1-有效
     */
    @ApiModelProperty(value = "状态:0-无效 1-有效")
    private Integer status;

    /**
     * 保留数据0-否 1-是 不允许删除
     */
    @ApiModelProperty(value = "保留数据0-否 1-是 不允许删除")
    private Boolean isPersist;

    /**
     * 服务ID
     */
    @ApiModelProperty(value = "服务ID")
    private String serviceId;

    /**
     * 对应的什么APP
     */
    @ApiModelProperty(value = "对应的APP")
    private Long appId;
}
