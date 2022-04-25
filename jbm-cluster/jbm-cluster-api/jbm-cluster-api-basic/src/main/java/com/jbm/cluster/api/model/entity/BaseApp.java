package com.jbm.cluster.api.model.entity;


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
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 系统应用-基础信息
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_app")
@ApiModel("应用管理")
@Table(name = "base_app", indexes = {@Index(name = "apiKeyIndex", columnList = "apiKey", unique = true)})
public class BaseApp extends MasterDataEntity {
    @Id
    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "应用ID")
    private String appId;

    /**
     * API访问key
     */
    @ApiModelProperty(value = "API访问key")
    private String apiKey;
    /**
     * API访问密钥
     */
    @ApiModelProperty(value = "API访问密钥")
    private String secretKey;

    /**
     * app类型：server-服务应用 app-手机应用 pc-PC网页应用 wap-手机网页应用
     */
    @ApiModelProperty(value = "app类型：server-服务应用 app-手机应用 pc-PC网页应用 wap-手机网页应用")
    private String appType;

    /**
     * 应用图标
     */
    @ApiModelProperty(value = "应用图标")
    private String appIcon;

    @ApiModelProperty(value = "多类型图标")
    private String appIcons;

    /**
     * app名称
     */
    @ApiModelProperty(value = "app名称")
    private String appName;

    /**
     * app英文名称
     */
    @ApiModelProperty(value = "app英文名称")
    private String appNameEn;
    /**
     * 移动应用操作系统：ios-苹果 android-安卓
     */
    @ApiModelProperty(value = "移动应用操作系统：ios-苹果 android-安卓")
    private String appOs;

    /**
     * 用户ID:默认为0
     */
    @ApiModelProperty(value = "用户ID:默认为0")
    private Long developerId;

    /**
     * app描述
     */
    @ApiModelProperty(value = "app描述")
    private String appDesc;

    /**
     * 官方网址
     */
    @ApiModelProperty(value = "官方网址")
    private String website;

    /**
     * 状态:0-无效 1-有效
     */
    @ApiModelProperty(value = "状态:0-无效 1-有效")
    private Integer status;

    /**
     * 保留数据0-否 1-是 不允许删除
     */
    @ApiModelProperty(value = "保留数据0-否 1-是 不允许删除")
    private Integer isPersist;

}
