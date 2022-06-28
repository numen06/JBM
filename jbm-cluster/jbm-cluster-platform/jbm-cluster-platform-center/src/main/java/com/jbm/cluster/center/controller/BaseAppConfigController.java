package com.jbm.cluster.center.controller;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.center.service.BaseAppConfigService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-06-27 12:55:11
 */
@Api(tags = "应用配置管理接口")
@RestController
@RequestMapping("/baseAppConfig")
public class BaseAppConfigController extends MasterDataCollection<BaseAppConfig, BaseAppConfigService> {

    @ApiOperation("获取应用配置")
    @GetMapping("/getAppConfigByKey")
    public ResultBody<String> getAppConfigByKey(@RequestParam(required = true) String appKey) {
        return ResultBody.callback(new Supplier<String>() {
            @Override
            public String get() {
                BaseAppConfig baseAppConfig = service.getAppConfigByKey(appKey);
                if (ObjectUtil.isEmpty(baseAppConfig)) {
                    return null;
                }
                return baseAppConfig.getConfigContent();
            }
        });
    }

}
