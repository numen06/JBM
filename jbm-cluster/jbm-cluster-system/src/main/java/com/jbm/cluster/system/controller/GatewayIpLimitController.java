package com.jbm.cluster.system.controller;

import com.jbm.cluster.api.model.entity.GatewayIpLimit;
import com.jbm.cluster.common.model.ResultBody;
import com.jbm.cluster.common.security.http.OpenRestTemplate;
import com.jbm.cluster.system.service.GatewayIpLimitService;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 网关IP访问控制
 *
 * @author: wesley.zhang
 * @date: 2019/3/12 15:12
 * @description:
 */
@Api(tags = "网关IP访问控制")
@RestController
public class GatewayIpLimitController {

    @Autowired
    private GatewayIpLimitService gatewayIpLimitService;
    @Autowired
    private OpenRestTemplate openRestTemplate;

    /**
     * 获取分页接口列表
     *
     * @return
     */
    @ApiOperation(value = "获取分页接口列表", notes = "获取分页接口列表")
    @GetMapping("/gateway/limit/ip")
    public ResultBody<DataPaging<GatewayIpLimit>> getIpLimitListPage(@RequestParam(required = false) JsonRequestBody jsonRequestBody) {
        return ResultBody.ok().data(gatewayIpLimitService.findListPage(jsonRequestBody));
    }

    /**
     * 查询策略已绑定API列表
     *
     * @param policyId
     * @return
     */
    @ApiOperation(value = "查询策略已绑定API列表", notes = "获取分页接口列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "策略ID", paramType = "form"),
    })
    @GetMapping("/gateway/limit/ip/api/list")
    public ResultBody<DataPaging<GatewayIpLimit>> getIpLimitApiList(
            @RequestParam("id") Long policyId
    ) {
        return ResultBody.ok().data(gatewayIpLimitService.findIpLimitApiList(policyId));
    }

    /**
     * 绑定API
     *
     * @param policyId 策略ID
     * @param apiIds   API接口ID.多个以,隔开.选填
     * @return
     */
    @ApiOperation(value = "绑定API", notes = "一个API只能绑定一个策略")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "策略ID", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "apiIds", value = "API接口ID.多个以,隔开.选填", defaultValue = "", required = false, paramType = "form")
    })
    @PostMapping("/gateway/limit/ip/api/add")
    public ResultBody addIpLimitApis(
            @RequestParam("id") Long policyId,
            @RequestParam(value = "apiIds", required = false) String apiIds
    ) {
        gatewayIpLimitService.addIpLimitApis(policyId, StringUtils.isNotBlank(apiIds) ? apiIds.split(",") : new String[]{});
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }

    /**
     * 获取IP限制
     *
     * @param policyId
     * @return
     */
    @ApiOperation(value = "获取IP限制", notes = "获取IP限制")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "策略ID", paramType = "path"),
    })
    @GetMapping("/gateway/limit/ip/{policyId}/info")
    public ResultBody<GatewayIpLimit> getIpLimit(@PathVariable("policyId") Long policyId) {
        return ResultBody.ok().data(gatewayIpLimitService.getIpLimitPolicy(policyId));
    }

    /**
     * 添加IP限制
     *
     * @param policyName 策略名称
     * @param policyType 策略类型:0-拒绝/黑名单 1-允许/白名单
     * @param ipAddress  ip地址/IP段:多个用隔开;最多10个
     * @return
     */
    @ApiOperation(value = "添加IP限制", notes = "添加IP限制")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "policyName", required = true, value = "策略名称", paramType = "form"),
            @ApiImplicitParam(name = "policyType", required = true, value = "策略类型:0-拒绝/黑名单 1-允许/白名单", allowableValues = "0,1", paramType = "form"),
            @ApiImplicitParam(name = "ipAddress", required = true, value = "ip地址/IP段:多个用隔开;最多10个", paramType = "form")
    })
    @PostMapping("/gateway/limit/ip/add")
    public ResultBody<Long> addIpLimit(
            @RequestParam(value = "policyName") String policyName,
            @RequestParam(value = "policyType") Integer policyType,
            @RequestParam(value = "ipAddress") String ipAddress
    ) {
        GatewayIpLimit ipLimit = new GatewayIpLimit();
        ipLimit.setPolicyName(policyName);
        ipLimit.setPolicyType(policyType);
        ipLimit.setIpAddress(ipAddress);
        Long policyId = null;
        GatewayIpLimit result = gatewayIpLimitService.addIpLimitPolicy(ipLimit);
        if(result!=null){
            policyId = result.getId();
        }
        return ResultBody.ok().data(policyId);
    }

    /**
     * 编辑IP限制
     *
     * @param policyId   IP限制ID
     * @param policyName 策略名称
     * @param policyType 策略类型:0-拒绝/黑名单 1-允许/白名单
     * @param ipAddress  ip地址/IP段:多个用隔开;最多10个
     * @return
     */
    @ApiOperation(value = "编辑IP限制", notes = "编辑IP限制")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "接口Id", paramType = "form"),
            @ApiImplicitParam(name = "policyName", required = true, value = "策略名称", paramType = "form"),
            @ApiImplicitParam(name = "policyType", required = true, value = "策略类型:0-拒绝/黑名单 1-允许/白名单", allowableValues = "0,1", paramType = "form"),
            @ApiImplicitParam(name = "ipAddress", required = true, value = "ip地址/IP段:多个用隔开;最多10个", paramType = "form")
    })
    @PostMapping("/gateway/limit/ip/update")
    public ResultBody updateIpLimit(
            @RequestParam("id") Long policyId,
            @RequestParam(value = "policyName") String policyName,
            @RequestParam(value = "policyType") Integer policyType,
            @RequestParam(value = "ipAddress") String ipAddress
    ) {
        GatewayIpLimit ipLimit = new GatewayIpLimit();
        ipLimit.setId(policyId);
        ipLimit.setPolicyName(policyName);
        ipLimit.setPolicyType(policyType);
        ipLimit.setIpAddress(ipAddress);
        gatewayIpLimitService.updateIpLimitPolicy(ipLimit);
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 移除IP限制
     *
     * @param policyId
     * @return
     */
    @ApiOperation(value = "移除IP限制", notes = "移除IP限制")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "id", paramType = "form"),
    })
    @PostMapping("/gateway/limit/ip/remove")
    public ResultBody removeIpLimit(
            @RequestParam("id") Long policyId
    ) {
        gatewayIpLimitService.removeIpLimitPolicy(policyId);
        // 刷新网关
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }
}
