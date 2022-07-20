package com.jbm.cluster.logs.controllers;


import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.form.GatewayLogsForm;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "日志接口")
@RestController
@RequestMapping("/GatewayLogs")
public class GatewayLogsController {
    /**
     * 临时存放减少io
     */
    @Autowired
    private GatewayLogsService gatewayLogsService;


    @ApiOperation(value = "获取多表查询分页列表")
    @PostMapping({"/findOperationLogs"})
    public ResultBody<DataPaging<GatewayLogs>> findOperationLogs(@RequestBody(required = false) GatewayLogsForm gatewayLogsForm) {
        try {
            DataPaging<GatewayLogs> dataPaging = gatewayLogsService.findLogs(gatewayLogsForm, true);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询日志失败", e);
        }
    }

    @ApiOperation(value = "获取多表查询分页列表")
    @PostMapping({"/findLogs"})
    public ResultBody<DataPaging<GatewayLogs>> findLogs(@RequestBody(required = false) GatewayLogsForm gatewayLogsForm) {
        try {
            DataPaging<GatewayLogs> dataPaging = gatewayLogsService.findLogs(gatewayLogsForm);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询日志失败", e);
        }
    }

}
