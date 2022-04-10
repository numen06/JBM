package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.BaseArea;
import com.jbm.cluster.api.model.entity.BaseDic;
import com.jbm.cluster.center.service.BaseAreaService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-04-07 21:44:18
 */
@Api(tags = "行政区域开放接口")
@RestController
@RequestMapping("/baseArea")
public class BaseAreaController extends MasterDataCollection<BaseArea, BaseAreaService> {

    @ApiOperation("获取数据字典")
    @GetMapping("/getChinaAreaMap")
    public ResultBody<Map<String, List<BaseArea>>> getChinaAreaMap() {
        Map<String, List<BaseArea>> result = this.service.getChinaAreaMap();
        return ResultBody.success(result, "获取数据字典成功");
    }

}
