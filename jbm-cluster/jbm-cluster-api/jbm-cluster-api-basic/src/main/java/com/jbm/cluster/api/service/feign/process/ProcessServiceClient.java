package com.jbm.cluster.api.service.feign.process;

import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.model.job.rule.*;
import com.jbm.cluster.api.form.job.ProcessInstancePageForm;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author scolin
 * @description 流程服务Feign客户端
 * @date 2025/12/10
 */
@Component
@FeignClient(value = JbmClusterConstants.JOB_SERVER, path = "/process")
public interface ProcessServiceClient {
    
    /**
     * 分页查询流程实例
     */
    @PostMapping("/selectPageList")
    ResultBody<DataPaging<RuleInstanceModel>> pageQueryProcessInstances(
            @RequestBody(required = false) ProcessInstancePageForm pageForm);
    
    /**
     * 根据ID查询流程实例
     */
    @PostMapping("/getDetail")
    ResultBody<RuleInstanceModel> getProcessInstanceById(
            @RequestBody(required = false) ProcessInstancePageForm pageForm);
    
    /**
     * 执行流程
     */
    @PostMapping("/execute")
    ResultBody<ExecuteProcessResponse> executeProcess(@RequestBody ExecuteProcessRequest request);
    
    /**
     * 根据json创建流程实例
     */
    @PostMapping("/createProcessByJson")
    ResultBody<ProcessInstance> createProcessByJson(
            @RequestBody ExecuteProcessByJsonRequest request);
    
    /**
     * 根据流程实例id执行流程
     */
    @PostMapping("/executeProcessById")
    ResultBody<ExecuteProcessResponse> executeProcessByJson(
            @RequestBody ExecuteProcessByJsonRequest request);
    
    /**
     * 继续执行等待中的流程
     */
    @PostMapping("/continue")
    ResultBody<ExecuteProcessResponse> continueProcess(@RequestBody ExecuteProcessRequest request);
    
    /**
     * 解析流程json
     */
    @PostMapping("/parseProcessByJson")
    ResultBody<FlowData> parseProcessByJson(
            @RequestBody ExecuteProcessByJsonRequest request);
}
