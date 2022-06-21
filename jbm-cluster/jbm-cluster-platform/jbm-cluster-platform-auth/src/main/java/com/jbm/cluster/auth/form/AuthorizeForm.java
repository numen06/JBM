package com.jbm.cluster.auth.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Created wesley.zhang
 * @Date 2022/6/7 12:30
 * @Description TODO
 */
@Data
@ApiModel
public class AuthorizeForm {
    @ApiModelProperty("登录名")
    private String username;
    @ApiModelProperty("密码")
    private String password;
    @ApiModelProperty("授权类型:authorization_code授权码模式;password密码模式;client_credentials客户端模式")
    private String grant_type;
    @ApiModelProperty("客户端ID")
    private String client_id;
    @ApiModelProperty("相应类型")
    private String response_type;
    @ApiModelProperty("回调地址")
    private String redirect_uri;
    @ApiModelProperty("作用域")
    private String scope;
    @ApiModelProperty("授权码")
    private String code;
    @ApiModelProperty("登录方式")
    private String loginType;
}
