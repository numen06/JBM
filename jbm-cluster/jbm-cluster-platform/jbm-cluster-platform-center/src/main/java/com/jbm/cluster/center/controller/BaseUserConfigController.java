package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseUserConfig;
import com.jbm.cluster.center.service.BaseUserConfigService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-08-25 11:19:05
 */
@Api(tags = "用户配置管理")
@RestController
@RequestMapping("/baseUserConfig")
public class BaseUserConfigController extends MasterDataCollection<BaseUserConfig, BaseUserConfigService> {
}
