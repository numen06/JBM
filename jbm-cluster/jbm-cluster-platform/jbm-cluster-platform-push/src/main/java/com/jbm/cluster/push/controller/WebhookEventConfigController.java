package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Api(tags = "Web反向推送配置开放接口")
@RestController
@RequestMapping("/webhookEventConfig")
public class WebhookEventConfigController extends MultiPlatformCollection<WebhookEventConfig, WebhookEventConfigService> {

    @ApiOperation(value = "保存配置")
    @PostMapping("/saveConfig")
    public ResultBody<WebhookEventConfig> saveConfig(@RequestBody(required = false) WebhookEventConfig webhookEventConfig) {
        WebhookEventConfig entity = service.saveEntity(webhookEventConfig);
        return ResultBody.success(entity, "保存对象成功");
    }

    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/findConfig")
    public ResultBody<WebhookEventConfig> findConfig(@RequestBody(required = false) final WebhookEventConfig entity) {
        return ResultBody.callback("查询成功", () -> {
            return service.selectEntity(entity);
        });
    }

}
