package com.jbm.cluster.center.controller;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.api.form.BaseApiKeyForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 第三方 API Key 管理
 */
@Api(tags = "API Key管理")
@RestController
@RequestMapping("/apikey")
public class BaseApiKeyController extends BaseController {

    @Autowired
    private BaseApiKeyService baseApiKeyService;

    @ApiOperation("API Key 列表")
    @GetMapping
    public ResultBody<DataPaging<BaseApiKey>> list(@ModelAttribute BaseApiKeyForm form) {
        final BaseApiKeyForm query = form != null ? form : new BaseApiKeyForm();
        if (query.getDeveloperId() == null) {
            query.setDeveloperId(LoginHelper.getUserId());
        }
        return ResultBody.callback(() -> baseApiKeyService.findListPage(query));
    }

    @ApiOperation("按 apiKey 查询（Gateway/Feign）")
    @GetMapping(params = "apiKey")
    public ResultBody<BaseApiKey> getByApiKey(@RequestParam String apiKey) {
        return ResultBody.callback(() -> baseApiKeyService.getByApiKey(apiKey));
    }

    @ApiOperation("API Key 详情")
    @GetMapping("/{keyId}")
    public ResultBody<BaseApiKey> get(@PathVariable Long keyId) {
        return ResultBody.callback(() -> baseApiKeyService.getByKeyId(keyId));
    }

    @ApiOperation("创建 API Key")
    @PostMapping
    public ResultBody<BaseApiKey> create(@RequestBody BaseApiKeyForm form) {
        return ResultBody.callback(() -> baseApiKeyService.createApiKey(form, LoginHelper.getUserId()));
    }

    @ApiOperation("更新 API Key")
    @PutMapping("/{keyId}")
    public ResultBody<BaseApiKey> update(@PathVariable Long keyId, @RequestBody BaseApiKeyForm form) {
        return ResultBody.callback(() -> baseApiKeyService.updateApiKey(keyId, form));
    }

    @ApiOperation("重置密钥")
    @PutMapping("/{keyId}/secret")
    public ResultBody<String> resetSecret(@PathVariable Long keyId) {
        return ResultBody.callback(() -> baseApiKeyService.resetSecret(keyId));
    }

    @ApiOperation("启用/禁用")
    @PutMapping("/{keyId}/status")
    public ResultBody<Void> updateStatus(@PathVariable Long keyId, @RequestBody Map<String, Integer> body) {
        baseApiKeyService.updateStatus(keyId, body.get("status"));
        return ResultBody.ok();
    }

    @ApiOperation("删除 API Key")
    @DeleteMapping("/{keyId}")
    public ResultBody<Void> delete(@PathVariable Long keyId) {
        baseApiKeyService.removeApiKey(keyId);
        return ResultBody.ok();
    }

    @ApiOperation("查看已授权接口")
    @GetMapping("/{keyId}/authority")
    public ResultBody<List<OpenAuthority>> getAuthority(@PathVariable Long keyId) {
        return ResultBody.callback(() -> baseApiKeyService.findAuthorityByKeyId(keyId));
    }

    @ApiOperation("设置授权")
    @PutMapping("/{keyId}/authority")
    public ResultBody<Void> grantAuthority(@PathVariable Long keyId, @RequestBody BaseApiKeyForm form) {
        String[] ids = form.getAuthorityIds() != null
                ? form.getAuthorityIds().toArray(new String[0])
                : new String[0];
        baseApiKeyService.grantAuthority(keyId, LoginHelper.getUserId(), form.getAuthorityExpireTime(), ids);
        return ResultBody.ok();
    }

    @ApiOperation("校验 API Key 是否有权访问 apiId（Gateway）")
    @GetMapping("/{keyId}/check")
    public ResultBody<Boolean> checkAuthority(@PathVariable Long keyId, @RequestParam Long apiId) {
        return ResultBody.callback(() -> baseApiKeyService.hasAuthorityForApi(keyId, apiId));
    }
}
