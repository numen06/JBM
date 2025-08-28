package com.jbm.cluster.job.controller;

import com.jbm.cluster.api.entitys.job.DynamicField;
import com.jbm.cluster.job.service.DynamicFieldService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
@Api(tags = "动态模版类字段开放接口")
@RestController
@RequestMapping("/dynamicField")
public class DynamicFieldController extends MasterDataCollection<DynamicField, DynamicFieldService> {
}
