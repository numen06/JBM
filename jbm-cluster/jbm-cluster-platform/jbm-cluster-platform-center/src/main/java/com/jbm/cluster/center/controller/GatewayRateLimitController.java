package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.gateway.GatewayRateLimit;
import com.jbm.cluster.api.entitys.gateway.GatewayRateLimitApi;
import com.jbm.cluster.api.form.GatewayRateLimitForm;
import com.jbm.cluster.center.business.GatewayRateLimitBusiness;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网关流量控制
 */
@Api(tags = "网关流量控制")
@RestController
@RequestMapping("/gateway/limit/rate")
public class GatewayRateLimitController {

    @Autowired
    private GatewayRateLimitBusiness gatewayRateLimitBusiness;

    @ApiOperation(value = "策略列表")
    @GetMapping
    public ResultBody<DataPaging<GatewayRateLimit>> listPolicies(@ModelAttribute GatewayRateLimitForm form) {
        return ResultBody.callback(() -> gatewayRateLimitBusiness.findListPage(
                form != null ? form : new GatewayRateLimitForm()));
    }

    @ApiOperation(value = "策略详情")
    @GetMapping("/{policyId}")
    public ResultBody<GatewayRateLimit> getPolicy(@PathVariable Long policyId) {
        return ResultBody.callback(() -> gatewayRateLimitBusiness.getRateLimitPolicy(policyId));
    }

    @ApiOperation(value = "策略绑定的 API")
    @GetMapping("/{policyId}/apis")
    public ResultBody<List<GatewayRateLimitApi>> listPolicyApis(@PathVariable Long policyId) {
        return ResultBody.callback(() -> gatewayRateLimitBusiness.findRateLimitApiList(policyId));
    }

    @ApiOperation(value = "创建策略")
    @PostMapping
    public ResultBody<Long> createPolicy(@RequestBody GatewayRateLimitForm form) {
        return ResultBody.callback(() -> gatewayRateLimitBusiness.addRateLimitWithGatewayRefresh(form));
    }

    @ApiOperation(value = "更新策略")
    @PutMapping("/{policyId}")
    public ResultBody<Void> updatePolicy(@PathVariable Long policyId, @RequestBody GatewayRateLimitForm form) {
        form.setPolicyId(policyId);
        gatewayRateLimitBusiness.updateRateLimitWithGatewayRefresh(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除策略")
    @DeleteMapping("/{policyId}")
    public ResultBody<Void> deletePolicy(@PathVariable Long policyId) {
        gatewayRateLimitBusiness.removeRateLimitWithGatewayRefresh(policyId);
        return ResultBody.ok();
    }

    @ApiOperation(value = "绑定 API")
    @PutMapping("/{policyId}/apis")
    public ResultBody<Void> putPolicyApis(
            @PathVariable Long policyId,
            @RequestParam(value = "apiIds", required = false) String apiIds) {
        gatewayRateLimitBusiness.addRateLimitApisWithGatewayRefresh(policyId,
                StringUtils.isNotBlank(apiIds) ? apiIds.split(",") : new String[]{});
        return ResultBody.ok();
    }
}
