package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.cluster.center.service.BaseAppConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2022-06-27 12:55:11
 */
@Api(tags = "应用配置管理接口")
@RestController
@RequestMapping("/baseAppConfig")
@SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
public class BaseAppConfigController extends MasterDataCollection<BaseAppConfig, BaseAppConfigService> {
    @PermitAll
    @ApiOperation("获取应用配置")
    @GetMapping("/getAppConfigByKey")
    public ResultBody<String> getAppConfigByKey(@RequestParam(required = true) String appKey) {
        return ResultBody.callback(() -> {
            BaseAppConfig baseAppConfig = service.getAppConfigByKey(appKey, null);
            if (ObjectUtil.isEmpty(baseAppConfig)) {
                return null;
            }
            if (ObjectUtil.isNotEmpty(LoginHelper.softGetLoginUser())) {
                // 超级管理员账号查询所有数据
                if (LoginHelper.isAdmin()) {
                    return baseAppConfig.getConfigContent();
                }
                baseAppConfig = service.getAppConfigByKey(appKey, LoginHelper.getLoginUser().getCompanyId());
            }
            return baseAppConfig.getConfigContent();
        });
    }

}
