package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.form.BaseAppForm;
import com.jbm.cluster.center.business.BaseAppBusiness;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统应用管理
 */
@Api(tags = "系统应用管理")
@RestController
@RequestMapping("/app")
public class BaseAppController extends BaseController {

    @Autowired
    private BaseAppBusiness baseAppBusiness;
    @Autowired
    private BaseAppService baseAppService;

    @ApiOperation(value = "按 apiKey 查询应用")
    @GetMapping(params = "apiKey")
    public ResultBody<BaseApp> getAppByApiKey(@RequestParam String apiKey) {
        return ResultBody.callback(() -> baseAppService.getAppInfoByKey(apiKey));
    }

    @ApiOperation(value = "应用列表（分页）")
    @GetMapping
    public ResultBody<DataPaging<BaseApp>> listApps(@ModelAttribute BaseAppForm form) {
        return ResultBody.callback(() -> baseAppService.findListPage(form != null ? form : new BaseAppForm()));
    }

    @ApiOperation(value = "应用详情")
    @GetMapping("/{appId}")
    public ResultBody<BaseApp> getApp(@PathVariable Long appId) {
        return ResultBody.callback(() -> baseAppService.getAppInfo(appId));
    }

    @ApiOperation(value = "创建应用")
    @PostMapping
    public ResultBody<Map<String, Object>> createApp(@RequestBody BaseAppForm form) {
        return ResultBody.callback(() -> {
            BaseApp result = baseAppBusiness.addAppWithGatewayRefresh(form);
            String clientSecret = baseAppBusiness.resetSecretWithGatewayRefresh(result.getAppId());
            Map<String, Object> credentials = new LinkedHashMap<>();
            credentials.put("appId", result.getAppId());
            credentials.put("clientId", result.getApiKey());
            credentials.put("clientSecret", clientSecret);
            return credentials;
        });
    }

    @ApiOperation(value = "更新应用")
    @PutMapping("/{appId}")
    public ResultBody<Void> updateApp(@PathVariable Long appId, @RequestBody BaseAppForm form) {
        baseAppBusiness.updateAppWithGatewayRefresh(appId, form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "更新应用客户端配置")
    @PutMapping("/{appId}/client")
    public ResultBody<Void> updateAppClient(@PathVariable Long appId, @RequestBody BaseAppForm form) {
        baseAppBusiness.updateAppWithGatewayRefresh(appId, form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "重置应用密钥")
    @PutMapping("/{appId}/secret")
    public ResultBody<String> resetAppSecret(@PathVariable Long appId) {
        return ResultBody.callback(() -> baseAppBusiness.resetSecretWithGatewayRefresh(appId));
    }

    @ApiOperation(value = "查看应用密钥")
    @GetMapping("/{appId}/secret")
    public ResultBody<String> getAppSecret(@PathVariable Long appId) {
        return ResultBody.callback(() -> baseAppBusiness.getPlainSecret(appId));
    }

    @ApiOperation(value = "删除应用")
    @DeleteMapping("/{appId}")
    public ResultBody<Void> deleteApp(@PathVariable Long appId) {
        baseAppBusiness.removeAppWithGatewayRefresh(appId);
        return ResultBody.ok();
    }
}
