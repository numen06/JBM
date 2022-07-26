package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
@Api(tags = "系统字典")
@RestController
@RequestMapping("/baseDic")
public class BaseDicController extends MasterDataTreeCollection<BaseDic, BaseDicService> {

    @ApiOperation("获取数据字典")
    @GetMapping("/getDicMap")
    public ResultBody<Map<String, List<BaseDic>>> getDicMap() {
        Map<String, List<BaseDic>> result = this.service.getDicMap();
        return ResultBody.success(result, "获取数据字典成功");
    }


}
