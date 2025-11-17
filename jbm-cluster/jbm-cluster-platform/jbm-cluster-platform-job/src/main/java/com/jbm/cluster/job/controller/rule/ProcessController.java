package com.jbm.cluster.job.controller.rule;

import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.constants.job.ProcessStatusEnum;
import com.jbm.cluster.api.model.job.rule.ExecuteProcessRequest;
import com.jbm.cluster.api.model.job.rule.ExecuteProcessResponse;
import com.jbm.cluster.job.business.impl.ProcessExecutionEngine;
import com.jbm.cluster.job.service.rule.ProcessInstanceService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 15:42
 */
@RestController
@RequestMapping("/api/process")
public class ProcessController extends MasterDataCollection<ProcessInstance, ProcessInstanceService> {
    @Autowired
    private ProcessExecutionEngine processExecutionEngine;

    @PostMapping("/execute")
    public ResponseEntity<ExecuteProcessResponse> executeProcess(@RequestBody ExecuteProcessRequest request) {
        try {
            ExecuteProcessResponse response = processExecutionEngine.executeProcess(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse(e.getMessage())
            );
        }
    }

    @PostMapping("/continue")
    public ResponseEntity<ExecuteProcessResponse> continueProcess(@RequestBody ExecuteProcessRequest request) {
        try {
            ExecuteProcessResponse response = processExecutionEngine.continueProcess(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse(e.getMessage())
            );
        }
    }

    private ExecuteProcessResponse createErrorResponse(String message) {
        ExecuteProcessResponse response = new ExecuteProcessResponse();
        response.setStatus(ProcessStatusEnum.FAILED.getValue());
        response.setMessage(message);
        return response;
    }
}
