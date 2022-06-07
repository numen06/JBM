package com.jbm.cluster.api.form.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("用户注册对象")
public class RegisterForm extends PasswordLoginWay {

    @ApiModelProperty(value = "用户类型")
    private String userType;

    /**
     * 头像
     */
    @ApiModelProperty("头像")
    private String avatar;

    /**
     * 邮箱
     */
    @ApiModelProperty("邮箱")
    private String email;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String mobile;

    /**
     * 昵称
     */
    @ApiModelProperty("昵称")
    private String nickName;

}
