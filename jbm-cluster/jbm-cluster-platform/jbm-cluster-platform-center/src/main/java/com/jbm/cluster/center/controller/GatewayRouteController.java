package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.center.business.GatewayRouteBusiness;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网关智能路由
 */
@Api(tags = "网关智能路由")
@RestController
@RequestMapping("/gateway/routes")
public class GatewayRouteController {

    @Autowired
    private GatewayRouteBusiness gatewayRouteBusiness;

    @ApiOperation(value = "微服务列表")
    @GetMapping("/micro-services")
    public ResultBody<List<String>> listMicroServices() {
        return ResultBody.callback(() -> gatewayRouteBusiness.getMicroServices());
    }

    @ApiOperation(value = "路由列表")
    @GetMapping
    public ResultBody<DataPaging<GatewayRoute>> listRoutes(@ModelAttribute GatewayRoutePageForm form) {
        return ResultBody.callback(() -> gatewayRouteBusiness.findListPage(form));
    }

    @ApiOperation(value = "路由详情")
    @GetMapping("/{routeId}")
    public ResultBody<GatewayRoute> getRoute(@PathVariable Long routeId) {
        return ResultBody.callback(() -> gatewayRouteBusiness.getRoute(routeId));
    }

    @ApiOperation(value = "创建路由")
    @PostMapping
    public ResultBody<Void> createRoute(@RequestBody GatewayRoute route) {
        gatewayRouteBusiness.addRouteWithGatewayRefresh(route);
        return ResultBody.ok();
    }

    @ApiOperation(value = "更新路由")
    @PutMapping("/{routeId}")
    public ResultBody<Void> updateRoute(@PathVariable Long routeId, @RequestBody GatewayRoute route) {
        route.setRouteId(routeId);
        gatewayRouteBusiness.updateRouteWithGatewayRefresh(route);
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除路由")
    @DeleteMapping("/{routeId}")
    public ResultBody<Void> deleteRoute(@PathVariable Long routeId) {
        gatewayRouteBusiness.removeRouteWithGatewayRefresh(routeId);
        return ResultBody.ok();
    }
}
