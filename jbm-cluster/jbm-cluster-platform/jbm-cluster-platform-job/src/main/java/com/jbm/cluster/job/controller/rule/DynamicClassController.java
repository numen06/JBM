package com.jbm.cluster.job.controller.rule;

import com.jbm.cluster.api.entitys.job.rule.DynamicClass;
import com.jbm.cluster.job.service.rule.DynamicClassService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
@Api(tags = "动态模版类开放接口")
@RestController
@RequestMapping("/dynamicClass")
public class DynamicClassController extends MasterDataCollection<DynamicClass, DynamicClassService> {
}
