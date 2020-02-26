package com.jbm.cluster.center.controller;

import com.google.common.collect.Maps;
import com.jbm.cluster.api.model.entity.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.cluster.common.security.OpenHelper;
import com.jbm.cluster.common.security.OpenUserDetails;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-02-25 03:47:52
 */
@Api(tags = "系统字典")
@RestController
@RequestMapping("/baseDic")
public class BaseDicController extends MasterDataTreeCollection<BaseDic, BaseDicService> {

    @ApiOperation("获取数据字典")
    @GetMapping("/getDicMap")
    public ResultBody<Map<String, List<BaseDic>>> getDicMap() {
        OpenUserDetails user = OpenHelper.getUser();
        List<BaseDic> listRoot = this.service.selectRootListById();
        Map<String, List<BaseDic>> result = Maps.newLinkedHashMap();
        for (int i = 0; i < listRoot.size(); i++) {
            String code = listRoot.get(i).getCode();
            Long id = listRoot.get(i).getId();
            List<BaseDic> listDic = this.service.selectListByParentId(id);
            result.put(code, listDic);
        }
        return ResultBody.success(result, "获取数据字典成功");
    }

}
