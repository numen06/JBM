package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseArea;
import com.jbm.cluster.api.service.IBaseAreaServiceClient;
import com.jbm.cluster.center.service.BaseAreaService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
public class BaseAreaController extends MasterDataCollection<BaseArea, BaseAreaService> implements IBaseAreaServiceClient {

    @ApiOperation("获取地区字典")
    @GetMapping("/getChinaAreaList")
    @Override
    public ResultBody<List<BaseArea>> getChinaAreaList() {
        List<BaseArea> result = this.service.getChinaAreaList();
        return ResultBody.success(result, "获取地区字典成功");
    }

}
