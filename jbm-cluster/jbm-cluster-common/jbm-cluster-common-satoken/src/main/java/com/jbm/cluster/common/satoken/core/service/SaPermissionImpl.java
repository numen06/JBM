package com.jbm.cluster.common.satoken.core.service;

import cn.dev33.satoken.stp.StpInterface;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * sa-token 权限管理实现类
 *
 * @author Lion Li
 */
//@Component
public class SaPermissionImpl implements StpInterface {

    /**
     * 获取菜单权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        JbmLoginUser loginUser = LoginHelper.getLoginUser(loginId);
//        UserType userType = UserType.getUserType(loginUser.getUserType());
//        if (userType == UserType.SYS_USER) {
        return Lists.newArrayList(loginUser.getMenuPermission());
//        } else if (userType == UserType.APP_USER) {
//             其他端 自行根据业务编写
//        }
//        return new ArrayList<>();
    }

    /**
     * 获取角色权限列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        JbmLoginUser loginUser = LoginHelper.getLoginUser();
//        UserType userType = UserType.getUserType(loginUser.getUserType());
//        if (userType == UserType.SYS_USER) {
        return Lists.newArrayList(loginUser.getRoles());
//        } else if (userType == UserType.APP_USER) {
        // 其他端 自行根据业务编写
//        }
//        return new ArrayList<>();
    }
}
