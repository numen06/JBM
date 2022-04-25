package com.jbm.cluster.api.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("第三方帐号信息")
@Data
public class ThirdPartyUserForm {

    @ApiModelProperty("授权账号")
    private String account;
    @ApiModelProperty("授权密码")
    private String password;
    @ApiModelProperty("授权类型")
    private String accountType;
    @ApiModelProperty("昵称")
    private String nickName;
    @ApiModelProperty("头像")
    private String avatar;
    @ApiModelProperty("手机号")
    private String phone;
}
