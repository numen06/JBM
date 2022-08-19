package com.jbm.cluster.logs.controllers;

import com.jbm.cluster.logs.form.ClusterAccessInfo;
import com.jbm.cluster.logs.service.ClusterAccessService;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "访问统计")
@RestController
@RequestMapping("/clusterAccess")
public class ClusterAccessController {

    @Autowired
    private ClusterAccessService clusterAccessService;

    @ApiOperation(value = "获取访问统计")
    @PostMapping({"/getClusterAccessInfo"})
    public ResultBody<ClusterAccessInfo> getClusterAccessInfo() {
        return ResultBody.callback("查询访问统计成功", () -> {
            ClusterAccessInfo clusterAccessInfo = clusterAccessService.getClusterAccessInfo();
            return clusterAccessInfo;
        });
    }


}
