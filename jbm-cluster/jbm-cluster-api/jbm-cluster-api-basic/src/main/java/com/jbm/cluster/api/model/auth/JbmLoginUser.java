package com.jbm.cluster.api.model.auth;


import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 用户信息
 *
 * @author wesley.zhang
 */
@Data
public class JbmLoginUser implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 用户名id
     */
    private Long userId;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 权限列表
     */
    private Set<String> permissions;

    /**
     * 菜单权限
     */
    private Set<String> menuPermission;


    /**
     * 角色权限
     */
    private Set<String> rolePermission;

    /**
     * 角色列表
     */
    private Set<String> roles;

    /**
     * 数据权限 当前角色ID
     */
    private Long roleId;

//    /**
//     * 用户信息
//     */
//    private BaseUser baseUser;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名
     */
    private String deptName;

    /**
     * 获取登录id
     */
    public String getLoginId() {
        if (StrUtil.isEmpty(userType)) {
            userType = "user";
        }
        return userType + JbmCacheConstants.LOGINID_JOIN_CODE + userId;
    }

}
