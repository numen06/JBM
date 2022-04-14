package com.jbm.cluster.api.service;

import com.jbm.cluster.api.model.entity.BaseArea;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/baseArea")
public interface IBaseAreaServiceClient {

    @ApiOperation("获取地区字典")
    @GetMapping("/getChinaAreaList")
    ResultBody<List<BaseArea>> getChinaAreaList();
}
