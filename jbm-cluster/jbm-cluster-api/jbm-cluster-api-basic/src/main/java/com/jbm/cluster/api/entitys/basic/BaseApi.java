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
 * 系统资源-API接口
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:21
 * @description:
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_api")
@ApiModel("API接口")
public class BaseApi extends MasterDataEntity {
    /**
     * 资源ID
     */
    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "资源ID")
    private Long apiId;

    /**
     * 资源编码
     */
    @ApiModelProperty(value = "资源编码")
    private String apiCode;

    /**
     * 资源名称
     */
    @ApiModelProperty(value = "资源名称")
    private String apiName;

    /**
     * 服务ID
     */
    @ApiModelProperty(value = "服务ID")
    private String serviceId;

    /**
     * 接口分类
     */
    @ApiModelProperty(value = "接口分类")
    private String apiCategory;

    /**
     * 资源路径
     */
    @ApiModelProperty(value = "资源路径")
    private String path;

    /**
     * 优先级
     */
    @ApiModelProperty(value = "优先级")
    private Integer priority;

    /**
     * 资源描述
     */
    @ApiModelProperty(value = "资源描述")
    private String apiDesc;

    /**
     * 状态:0-无效 1-有效
     */
    @ApiModelProperty(value = "0-无效 1-有效")
    private Integer status;

    /**
     * 保留数据0-否 1-是 不允许删除
     */
    @ApiModelProperty(value = "保留数据0-否 1-是 不允许删除")
    private Boolean isPersist;

    /**
     * 安全认证:0-否 1-是 默认:1
     */
    @ApiModelProperty(value = "安全认证:0-否 1-是 默认:1")
    private Boolean isAuth;

    /**
     * 是否公开访问: 0-内部的 1-公开的
     */
    @ApiModelProperty(value = "是否公开访问: 0-内部的 1-公开的")
    private Integer isOpen;
    /**
     * 请求方式
     */
    @ApiModelProperty(value = "请求方式")
    private String requestMethod;
    /**
     * 响应类型
     */
    @ApiModelProperty(value = "响应类型")
    private String contentType;

    /**
     * 类名
     */
    @ApiModelProperty(value = "类名")
    private String className;

    /**
     * 方法名
     */
    @ApiModelProperty(value = "方法名")
    private String methodName;

    @ApiModelProperty(value = "是否记录访问日志")
    private Boolean accessLog;

    @ApiModelProperty(value = "业务模块")
    private String businessScope;

}
