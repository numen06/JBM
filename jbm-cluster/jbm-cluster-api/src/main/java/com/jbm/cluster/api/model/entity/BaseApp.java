package com.jbm.cluster.api.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.annotation.TableAlias;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

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
@TableAlias("app")
public class BaseApp extends MasterDataEntity {
    @Id
    @TableId(type = IdType.INPUT)
    private String appId;

    /**
     * API访问key
     */
    private String apiKey;
    /**
     * API访问密钥
     */
    private String secretKey;

    /**
     * app类型：server-服务应用 app-手机应用 pc-PC网页应用 wap-手机网页应用
     */
    private String appType;

    /**
     * 应用图标
     */
    private String appIcon;

    /**
     * app名称
     */
    private String appName;

    /**
     * app英文名称
     */
    private String appNameEn;
    /**
     * 移动应用操作系统：ios-苹果 android-安卓
     */
    private String appOs;


    /**
     * 用户ID:默认为0
     */
    private Long developerId;

    /**
     * app描述
     */
    private String appDesc;

    /**
     * 官方网址
     */
    private String website;

    /**
     * 状态:0-无效 1-有效
     */
    private Integer status;

    /**
     * 保留数据0-否 1-是 不允许删除
     */
    private Integer isPersist;

}
