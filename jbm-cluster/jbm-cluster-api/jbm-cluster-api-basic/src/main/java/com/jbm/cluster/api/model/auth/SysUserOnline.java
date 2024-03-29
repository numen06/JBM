package com.jbm.cluster.api.model.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 当前在线会话
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@ApiModel("当前在线会话业务对象")
public class SysUserOnline implements Serializable {

    /**
     * 会话编号
     */
    @ApiModelProperty(value = "会话编号")
    private String tokenId;

    /**
     * 部门名称
     */
    @ApiModelProperty(value = "部门名称")
    private String deptName;

    /**
     * 用户名称
     */
    @ApiModelProperty(value = "用户名称")
    private String userName;

    /**
     * 登录IP地址
     */
    @ApiModelProperty(value = "登录IP地址")
    private String ipaddr;

    /**
     * 登录地址
     */
    @ApiModelProperty(value = "登录地址")
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @ApiModelProperty(value = "浏览器类型")
    private String browser;

    /**
     * 操作系统
     */
    @ApiModelProperty(value = "操作系统")
    private String os;

    /**
     * 登录时间
     */
    @ApiModelProperty(value = "登录时间")
    private Date loginTime;

    @ApiModelProperty(value = "过期时间")
    private Date expiredTime;

    @ApiModelProperty(value = "临时有效期")
    private Date activityTime;

}
