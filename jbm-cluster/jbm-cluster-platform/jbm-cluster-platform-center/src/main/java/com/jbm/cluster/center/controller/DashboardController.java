package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.jbm.cluster.api.model.dashboard.DashboardOverview;
import com.jbm.cluster.center.business.DashboardBusiness;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘聚合接口
 */
@Api(tags = "仪表盘")
@RestController
@RequestMapping("/current")
public class DashboardController {

    @Autowired
    private DashboardBusiness dashboardBusiness;

    @SaCheckLogin
    @ApiOperation(value = "当前用户仪表盘概览")
    @GetMapping("/dashboard")
    public ResultBody<DashboardOverview> getDashboardOverview() {
        return ResultBody.callback(dashboardBusiness::buildOverview);
    }
}
