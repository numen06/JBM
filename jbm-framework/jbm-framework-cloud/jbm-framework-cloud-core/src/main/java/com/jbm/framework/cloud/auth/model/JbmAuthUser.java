package com.jbm.framework.cloud.auth.model;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.List;

/**
 * @author: create by wesley
 * @date:2019/5/19
 */
@Data
public class JbmAuthUser {

    private static final long serialVersionUID = 1L;
    private Long userId;
    private String username;
    private String password;
    private List<String> roleList;
    private String identifyCode;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

    private boolean enabled;

    public JbmAuthUser() {
        super();
    }

    public static JbmAuthUser getInstances(OAuth2Authentication principal) {
        return JSON.parseObject(JSON.toJSONString(principal.getUserAuthentication().getDetails()), JbmAuthUser.class);
    }
}
