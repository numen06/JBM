package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-02-25 03:47:52
 */
@Api(tags = "系统字典")
@RestController
@RequestMapping("/baseDic")
public class BaseDicController extends MasterDataTreeCollection<BaseDic, BaseDicService> {
}
