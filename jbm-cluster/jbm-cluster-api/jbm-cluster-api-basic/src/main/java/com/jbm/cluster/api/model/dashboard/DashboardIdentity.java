package com.jbm.cluster.api.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 仪表盘当前登录身份摘要
 */
@Data
@ApiModel("仪表盘身份摘要")
public class DashboardIdentity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户 ID")
    private Long userId;

    @ApiModelProperty("登录名")
    private String userName;

    @ApiModelProperty("昵称")
    private String nickName;

    @ApiModelProperty("角色编码列表")
    private List<String> roles;

    @ApiModelProperty("当前应用 ID")
    private Long appId;

    @ApiModelProperty("OAuth 客户端 ID")
    private String clientId;

    @ApiModelProperty("可见菜单数量")
    private Integer visibleMenuCount;

    @ApiModelProperty("数据范围：platform 平台汇总 / app 当前应用")
    private String scope;
}
