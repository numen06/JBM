package com.jbm.cluster.center.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.jbm.cluster.center.service.GatewayAccessLogsService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 网关智能路由
 *
 * @author: wesley.zhang
 * @date: 2019/3/12 15:12
 * @description:
 */
@Api(tags = "网关访问日志")
@RestController
public class GatewayAccessLogsController {

    @Autowired
    private GatewayAccessLogsService gatewayAccessLogsService;

    /**
     * 获取分页列表
     *
     * @return
     */
    @ApiOperation(value = "获取分页访问日志列表", notes = "获取分页访问日志列表")
    @GetMapping("/gateway/access/logs")
    public ResultBody<IPage<GatewayAccessLogs>> getAccessLogListPage(@RequestParam(required = false) Map map) {
        return ResultBody.ok().data(gatewayAccessLogsService.findListPage(PageRequestBody.from(map)));
    }

}
