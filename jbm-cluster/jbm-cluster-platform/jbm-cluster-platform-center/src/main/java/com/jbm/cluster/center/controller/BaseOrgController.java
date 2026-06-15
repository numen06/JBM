package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.common.mysql.service.BaseOrgService;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
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
public class BaseOrgController extends MasterDataTreeCollection<BaseOrg, BaseOrgService> {

    @ApiOperation(value = "获取组织树", notes = "返回根节点列表，子组织嵌套在 children 中")
    @PostMapping("/tree")
    @Override
    public ResultBody<List<BaseOrg>> tree(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            BaseOrg entity = validatorMasterData(masterDataRequsetBody, false);
            List<BaseOrg> list = this.service.selectOrgTree(entity);
            return ResultBody.success(list, "查询树结构成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询树结构失败", e);
        }
    }

    @ApiOperation(value = "获取顶层公司", notes = "获取顶层公司")
    @PostMapping("/findTopCompany")
    public ResultBody<BaseOrg> findTopCompany(@RequestBody BaseOrg baseOrg) {
        return ResultBody.callback(() -> this.service.findTopCompany(baseOrg));
    }


    @ApiOperation(value = "获取下级公司", notes = "获取下级公司")
    @PostMapping("/findRelegationCompany")
    public ResultBody<List<BaseOrg>> findRelegationCompany(@RequestBody BaseOrg baseOrg) {
        return ResultBody.callback(() -> this.service.findRelegationCompany(baseOrg));
    }

    @ApiOperation(value = "获取组织信息", notes = "获取组织信息")
    @PostMapping("/getBaseOrg")
    public ResultBody<BaseOrg> getBaseOrg(@RequestBody BaseOrg baseOrg) {
        return ResultBody.callback(() -> this.service.getBaseOrg(baseOrg));
    }
}
