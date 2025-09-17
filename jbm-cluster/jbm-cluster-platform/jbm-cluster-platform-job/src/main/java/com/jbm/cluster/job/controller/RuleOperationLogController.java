package com.jbm.cluster.job.controller;

import com.jbm.cluster.api.entitys.job.RuleOperationLog;
import com.jbm.cluster.job.service.RuleOperationLogService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 18:29:38
 */
@Api(tags = "规则操作日志开放接口")
@RestController
@RequestMapping("/ruleOperationLog")
public class RuleOperationLogController extends MasterDataCollection<RuleOperationLog, RuleOperationLogService> {
}
