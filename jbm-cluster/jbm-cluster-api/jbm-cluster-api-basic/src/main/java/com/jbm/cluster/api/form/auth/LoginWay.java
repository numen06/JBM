package com.jbm.cluster.api.form.auth;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Created wesley.zhang
 * @Date 2022/6/2 15:49
 * @Description TODO
 */
@Data
@NoArgsConstructor
@ApiModel("用户登录帐户")
public abstract class LoginWay {

    public abstract String getAccountType();

    public abstract String getAccount();


}
