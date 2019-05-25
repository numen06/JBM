package com.jbm.framework.cloud.auth.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: create by wesley
 * @date:2019/5/19
 */
@Data
public class JbmAuthUser extends User {

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
        super("jbm", "jbm", Lists.newArrayList());
    }

//    public User toUserDetail() {
//        List<GrantedAuthority> authorities = new ArrayList<>();
//        for (String role : roleList) {
//            GrantedAuthority grantedAuthority = new GrantedAuthority() {
//                @Override
//                public String getAuthority() {
//                    return role;
//                }
//            };
//            authorities.add(grantedAuthority);
//        }
//        User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
//        return user;
//    }


    public static JbmAuthUser getInstances(OAuth2Authentication principal) {
        return JSON.parseObject(JSON.toJSONString(principal.getUserAuthentication().getDetails()), JbmAuthUser.class);
    }


}
