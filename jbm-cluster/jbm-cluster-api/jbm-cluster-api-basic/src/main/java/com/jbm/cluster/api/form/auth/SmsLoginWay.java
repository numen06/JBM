package com.jbm.cluster.api.form.auth;

import com.jbm.cluster.api.constants.AccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 短信登录对象
 *
 * @author Lion Li
 */

@Data
@ApiModel("短信登录对象")
public class SmsLoginWay extends LoginWay {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.mobile.not.blank}")
    @ApiModelProperty(value = "用户手机号")
    private String mobile;

    /**
     * 用户密码
     */
    @NotBlank(message = "{sms.code.not.blank}")
    @ApiModelProperty(value = "短信验证码")
    private String smsCode;

    @Override
    public String getAccountType() {
        return AccountType.sms.toString();
    }

    @Override
    public String getAccount() {
        return this.mobile;
    }
}
