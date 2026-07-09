package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.basic.BaseArea;
import com.jbm.cluster.core.annotation.InnerAuth;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface IBaseAreaServiceClient {

    @InnerAuth
    @ApiOperation("获取地区字典")
    @GetMapping("/getChinaAreaList")
    ResultBody<List<BaseArea>> getChinaAreaList();
}
