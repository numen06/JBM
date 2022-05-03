package com.jbm.cluster.api.model.auth;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义认证用户信息
 *
 * @author wesley.zhang
 */
@Data
public class OpenUserDetails {
    private static final long serialVersionUID = -123308657146774881L;


    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 登录名
     */
    private String username;
    /**
     * 密码
     */
    private String password;

    /**
     * 用户权限
     */
    private Collection<OpenAuthority> authorities = new ArrayList<>();
    /**
     * 是否已锁定
     */
    private boolean accountNonLocked;
    /**
     * 是否已过期
     */
    private boolean accountNonExpired;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 密码是否已过期
     */
    private boolean credentialsNonExpired;
    /**
     * 认证客户端ID
     */
    private String clientId;
    /**
     * 认证中心域,适用于区分多用户源,多认证中心域
     */
    private String domain;
    /**
     * 真实名字
     */
    private String realName;
    /**
     * 手机号
     */
    private String mobile;

    /**
     * 公司
     */
    private Long companyId;

    /**
     * 部门
     */
    private Long departmentId;

    /**
     * 昵称
     */
    private String nickName;
    /**
     * 头像
     */
    private String avatar;

    /**
     * 账户Id
     */
    private Long accountId;

    /***
     * 账户类型
     */
    private String accountType;

    /**
     * 用户附加属性
     */
    private Map<String, Object> attrs = new HashMap<>();


    /**
     * 只是客户端模式.不包含用户信息
     *
     * @return
     */
    public Boolean isClientOnly() {
        return clientId != null && username == null;
    }

}
