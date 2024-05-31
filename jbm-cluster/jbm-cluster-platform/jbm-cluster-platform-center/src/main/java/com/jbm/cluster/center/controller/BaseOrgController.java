package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.center.service.BaseOrgService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
@Api(tags = "组织结构管理")
@RestController
@RequestMapping("/baseOrg")
public class BaseOrgController extends MultiPlatformCollection<BaseOrg, BaseOrgService> {

    @ApiOperation(value = "获取顶层公司", notes = "获取顶层公司")
    @PostMapping("/findTopCompany")
    public ResultBody<BaseOrg> findTopCompany(@RequestBody BaseOrg baseOrg) {
        return ResultBody.ok().data(this.service.findTopCompany(baseOrg));
    }


    @ApiOperation(value = "获取下级公司", notes = "获取下级公司")
    @PostMapping("/findRelegationCompany")
    public ResultBody<List<BaseOrg>> findRelegationCompany(@RequestBody BaseOrg baseOrg) {
        return ResultBody.ok().data(this.service.findRelegationCompany(baseOrg));
    }
}
