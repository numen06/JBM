package com.jbm.cluster.job.controller.rule;

import cn.hutool.core.lang.Assert;
import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.constants.job.ProcessStatusEnum;
import com.jbm.cluster.api.form.job.AgvTestForm;
import com.jbm.cluster.api.form.job.ProcessInstancePageForm;
import com.jbm.cluster.api.model.job.rule.*;
import com.jbm.cluster.job.business.impl.ProcessExecutionEngine;
import com.jbm.cluster.job.service.rule.ProcessInstanceService;
import com.jbm.cluster.job.service.rule.RuleDefinitionService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 15:42
 */
@RestController
@RequestMapping("/process")
public class ProcessController extends MasterDataCollection<ProcessInstance, ProcessInstanceService> {
    @Autowired
    private ProcessExecutionEngine processExecutionEngine;

    @Autowired
    private RuleDefinitionService ruleDefinitionService;

    @ApiOperation(value = "分页查询流程实例", notes = "分页查询流程实例，包含关联的规则信息和节点执行信息")
    @PostMapping("/selectPageList")
    public ResultBody<DataPaging<RuleInstanceModel>> pageQueryProcessInstances(
            @RequestBody(required = false) ProcessInstancePageForm pageForm) {
        try {
            // 如果pageForm为空，使用默认分页参数
            if (pageForm == null) {
                pageForm = new ProcessInstancePageForm();
                pageForm.setPageForm(new com.jbm.framework.usage.paging.PageForm(1, 10));
            }
            // 剆处理pageForm为空的情况
            if (pageForm.getPageForm() == null) {
                pageForm.setPageForm(new com.jbm.framework.usage.paging.PageForm(1, 10));
            }
            DataPaging<RuleInstanceModel> result = this.service.pageQueryProcessInstances(
                    pageForm.getRuleDefinitionId(),
                    pageForm.getStatus(),
                    pageForm.getPageForm());
            return ResultBody.success(result, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "根据ID查询流程实例", notes = "根据ID查询流程实例详情，包含关联的规则信息和节点执行信息")
    @PostMapping("/getDetail")
    public ResultBody<RuleInstanceModel> getProcessInstanceById(
            @RequestBody(required = false) ProcessInstancePageForm pageForm) {
        try {
            Assert.notNull(pageForm.getId(), "流程实例ID不能为空");
            RuleInstanceModel result = this.service.getProcessInstanceById(pageForm.getId());
            if (result == null) {
                return ResultBody.error("流程实例不存在");
            }
            return ResultBody.success(result, "查询成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "执行流程", notes = "使用本地规则定义")
    @PostMapping("/execute")
    public ResultBody<ExecuteProcessResponse> executeProcess(@RequestBody ExecuteProcessRequest request) {
        try {
            ExecuteProcessResponse response = processExecutionEngine.executeProcess(request);
            // 根据流程执行结果返回对应的ResponseBody
            if (ProcessStatusEnum.FAILED.getCode().equals(response.getStatus())) {
                // 流程执行失败
                return ResultBody.failed(response)
                        .code(500)
                        .msg("流程执行失败");
            } else if (ProcessStatusEnum.WAITING.getCode().equals(response.getStatus())) {
                // 流程等待中
                return ResultBody.ok(response)
                        .msg("流程等待触发");
            } else {
                // 流程执行成功或其他状态
                return ResultBody.ok(response)
                        .msg("流程执行成功");
            }
        } catch (Exception e) {
            ExecuteProcessResponse errorResponse = new ExecuteProcessResponse();
            errorResponse.setStatus(ProcessStatusEnum.FAILED.getCode());
            errorResponse.setMessage("流程执行异常: " + e.getMessage());
            return ResultBody.failed(errorResponse)
                    .code(500)
                    .msg("流程执行异常: " + e.getMessage());
        }
    }

    @ApiOperation(value = "根据json创建流程实例", notes = "外部传入流程JSON，不使用本地规则定义")
    @PostMapping("/createProcessByJson")
    public ResultBody<ProcessInstance> createProcessByJson(
            @RequestBody ExecuteProcessByJsonRequest request) {

        ProcessInstance processInstance = processExecutionEngine.createProcessByJson(request);
        return ResultBody.success(processInstance, "流程实例创建成功");
    }

    @ApiOperation(value = "根据流程实例id执行流程")
    @PostMapping("/executeProcessById")
    public ResultBody<ExecuteProcessResponse> executeProcessByJson(
            @RequestBody ExecuteProcessByJsonRequest request) {
        try {
            ExecuteProcessResponse response = processExecutionEngine.executeProcessByJson(request);
            // 根据流程执行结果返回对应的ResponseBody
            if (ProcessStatusEnum.FAILED.getCode().equals(response.getStatus())) {
                // 流程执行失败
                return ResultBody.failed(response)
                        .code(500)
                        .msg("流程执行失败");
            } else if (ProcessStatusEnum.WAITING.getCode().equals(response.getStatus())) {
                // 流程等待中
                return ResultBody.ok(response)
                        .msg("流程等待触发");
            } else {
                // 流程执行成功或其他状态
                return ResultBody.ok(response)
                        .msg("流程执行成功");
            }
        } catch (Exception e) {
            ExecuteProcessResponse errorResponse = new ExecuteProcessResponse();
            errorResponse.setStatus(ProcessStatusEnum.FAILED.getCode());
            errorResponse.setMessage("流程执行异常: " + e.getMessage());
            return ResultBody.failed(errorResponse)
                    .code(500)
                    .msg("流程执行异常: " + e.getMessage());
        }
    }

    @PostMapping("/continue")
    public ResponseEntity<ExecuteProcessResponse> continueProcess(@RequestBody ExecuteProcessRequest request) {
        try {
            ExecuteProcessResponse response = processExecutionEngine.continueProcess(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse(e.getMessage()));
        }
    }

    @ApiOperation(value = "解析流程json")
    @PostMapping("/parseProcessByJson")
    public ResultBody<FlowData> parseProcessByJson(
            @RequestBody ExecuteProcessByJsonRequest request) {
        Assert.notNull(request.getRuleContent(), "流程JSON不能为空");
        FlowData flowData = processExecutionEngine.parseFlowData(request.getRuleContent());
        return ResultBody.success(flowData, "解析流程json成功");
    }

    @PostMapping("/agvTest")
    public ResultBody<String> agvTest(@RequestBody AgvTestForm form){
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(form);
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        return ResultBody.success();
    }

    @PostMapping("/alarmTest")
    public ResultBody<String> agvTest(){
        return ResultBody.success();
    }

    private ExecuteProcessResponse createErrorResponse(String message) {
        ExecuteProcessResponse response = new ExecuteProcessResponse();
        response.setStatus(ProcessStatusEnum.FAILED.getCode());
        response.setMessage(message);
        return response;
    }
}
