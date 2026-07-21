package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.BaseRoleUser;
import com.jbm.cluster.center.service.BaseRoleUserService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.mvc.web.MasterDataCollection;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:57:09
 */
@RestController
@RequestMapping("/baseRoleUser")
@SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
public class BaseRoleUserController extends MasterDataCollection<BaseRoleUser, BaseRoleUserService> {
}
