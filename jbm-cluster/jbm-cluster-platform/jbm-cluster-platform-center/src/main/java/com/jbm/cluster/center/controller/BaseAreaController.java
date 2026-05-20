package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseArea;
import com.jbm.cluster.api.service.IBaseAreaServiceClient;
import com.jbm.cluster.common.mysql.service.BaseAreaService;
import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-04-07 21:44:18
 */
@Api(tags = "行政区域开放接口")
@RestController
@RequestMapping("/baseArea")
public class BaseAreaController extends BaseController implements IBaseAreaServiceClient {

    @Autowired
    private BaseAreaService baseAreaService;

    @ApiOperation("获取地区字典")
    @GetMapping("/getChinaAreaList")
    @PermitAll
    @Override
    public ResultBody<List<BaseArea>> getChinaAreaList() {
        List<BaseArea> result = baseAreaService.getChinaAreaList();
        return ResultBody.success(result, "获取地区字典成功");
    }

}
