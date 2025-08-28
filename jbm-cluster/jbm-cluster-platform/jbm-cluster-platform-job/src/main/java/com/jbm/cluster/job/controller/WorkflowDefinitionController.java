package com.jbm.cluster.job.controller;

import com.jbm.cluster.api.entitys.job.WorkflowDefinition;
import com.jbm.cluster.job.service.WorkflowDefinitionService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-26 14:07:25
 */
@Api(tags = "工作流实体开放接口")
@RestController
@RequestMapping("/workflowDefinition")
public class WorkflowDefinitionController extends MasterDataCollection<WorkflowDefinition, WorkflowDefinitionService> {
}
