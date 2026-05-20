package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.gateway.GatewayIpLimit;
import com.jbm.cluster.api.entitys.gateway.GatewayIpLimitApi;
import com.jbm.cluster.api.form.GatewayIpLimitForm;
import com.jbm.cluster.center.business.GatewayIpLimitBusiness;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网关 IP 访问控制
 */
@Api(tags = "网关IP访问控制")
@RestController
@RequestMapping("/gateway/limit/ip")
public class GatewayIpLimitController {

    @Autowired
    private GatewayIpLimitBusiness gatewayIpLimitBusiness;

    @ApiOperation(value = "策略列表")
    @GetMapping
    public ResultBody<DataPaging<GatewayIpLimit>> listPolicies(@ModelAttribute GatewayIpLimitForm form) {
        return ResultBody.callback(() -> gatewayIpLimitBusiness.findListPage(
                form != null ? form : new GatewayIpLimitForm()));
    }

    @ApiOperation(value = "策略详情")
    @GetMapping("/{policyId}")
    public ResultBody<GatewayIpLimit> getPolicy(@PathVariable Long policyId) {
        return ResultBody.callback(() -> gatewayIpLimitBusiness.getIpLimitPolicy(policyId));
    }

    @ApiOperation(value = "策略绑定的 API")
    @GetMapping("/{policyId}/apis")
    public ResultBody<List<GatewayIpLimitApi>> listPolicyApis(@PathVariable Long policyId) {
        return ResultBody.callback(() -> gatewayIpLimitBusiness.findIpLimitApiList(policyId));
    }

    @ApiOperation(value = "创建策略")
    @PostMapping
    public ResultBody<Long> createPolicy(@RequestBody GatewayIpLimitForm form) {
        return ResultBody.callback(() -> gatewayIpLimitBusiness.addIpLimitWithGatewayRefresh(form));
    }

    @ApiOperation(value = "更新策略")
    @PutMapping("/{policyId}")
    public ResultBody<Void> updatePolicy(@PathVariable Long policyId, @RequestBody GatewayIpLimitForm form) {
        form.setPolicyId(policyId);
        gatewayIpLimitBusiness.updateIpLimitWithGatewayRefresh(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除策略")
    @DeleteMapping("/{policyId}")
    public ResultBody<Void> deletePolicy(@PathVariable Long policyId) {
        gatewayIpLimitBusiness.removeIpLimitWithGatewayRefresh(policyId);
        return ResultBody.ok();
    }

    @ApiOperation(value = "绑定 API")
    @PutMapping("/{policyId}/apis")
    public ResultBody<Void> putPolicyApis(
            @PathVariable Long policyId,
            @RequestParam(value = "apiIds", required = false) String apiIds) {
        gatewayIpLimitBusiness.addIpLimitApisWithGatewayRefresh(policyId,
                StringUtils.isNotBlank(apiIds) ? apiIds.split(",") : new String[]{});
        return ResultBody.ok();
    }
}
