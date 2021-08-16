package com.jbm.cluster.auth.service;

import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.common.security.OpenUserDetails;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-29 05:08
 **/
public class AccountUtils {

    public static OpenUserDetails setAuthorize(UserAccount account, String username) {
        OpenUserDetails userDetails = new OpenUserDetails();
        setAuthorize(userDetails, account, username);
        return userDetails;
    }

    /**
     * 设置授权信息
     */
    public static void setAuthorize(OpenUserDetails userDetails, UserAccount account, String username) {
        userDetails.setDomain(account.getDomain());
        userDetails.setAccountId(account.getAccountId());
        userDetails.setUserId(account.getUserId());
        userDetails.setUsername(username);
        userDetails.setPassword(account.getPassword());
        userDetails.setNickName(account.getNickName());
        userDetails.setAuthorities(account.getAuthorities());
        userDetails.setAvatar(account.getAvatar());
        userDetails.setAccountId(account.getAccountId());
        userDetails.setAccountType(account.getAccountType());

        boolean accountNonLocked = account.getStatus().intValue() != BaseConstants.ACCOUNT_STATUS_LOCKED;
        boolean credentialsNonExpired = true;
        boolean enabled = account.getStatus().intValue() == BaseConstants.ACCOUNT_STATUS_NORMAL ? true : false;
        boolean accountNonExpired = true;

        userDetails.setAccountNonLocked(accountNonLocked);
        userDetails.setAccountNonExpired(accountNonExpired);
        userDetails.setCredentialsNonExpired(credentialsNonExpired);
        userDetails.setEnabled(enabled);

//        userDetails.setClientId(clientProperties.getOauth2().get("admin").getClientId());
    }
}
