package com.jbm.cluster.api.form.auth;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.core.constant.UserConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录对象
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@ApiModel("用户名帐号")
public class PasswordLoginWay extends LoginWay {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH, message = "{user.username.length.valid}")
    @ApiModelProperty(value = "用户名")
    private String userName;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = UserConstants.PASSWORD_MIN_LENGTH, max = UserConstants.PASSWORD_MAX_LENGTH, message = "{user.password.length.valid}")
    @ApiModelProperty(value = "用户密码")
    private String password;


    @Override
    public String getAccountType() {
        if (ObjectUtil.isEmpty(this.userName)) {
            return null;
        } else if (Validator.isEmail(this.userName)) {
            return AccountType.email.toString();
        } else if (Validator.isMobile(this.userName)) {
            return AccountType.mobile.toString();
        } else {
            return AccountType.username.toString();
        }
    }

    @Override
    public String getAccount() {
        return this.getUserName();
    }

}
