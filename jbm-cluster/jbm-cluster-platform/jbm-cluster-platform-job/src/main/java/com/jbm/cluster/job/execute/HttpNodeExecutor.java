package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author scolin
 * @description HTTP节点执行器
 * @date 2025/10/22 11:36
 */
@Component
public class HttpNodeExecutor implements NodeExecutor {
    @Resource
    private RestTemplate restTemplate;

    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        try {
            Map<String, Object> nodeData = node.getData();
            String url = (String) nodeData.get("url");
            String method = (String) nodeData.get("method");
            String requestBodyType = (String) nodeData.get("requestBodyType");

            // 构建请求参数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            //token认证
            if(nodeData.get("token") != null){
                //前端有传入就使用输入的token
                headers.set("Authorization", nodeData.get("token").toString());
            }else {
                //使用系统操作人的token
                headers.set("Authorization", "Bearer " + SecurityUtils.getToken());
            }

            // 替换模板变量
            //String requestBody = buildRequestBody(nodeData, inputData);
            String requestBody = (String)nodeData.get("requestBody");
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 执行HTTP请求
            ResponseEntity<String> response = restTemplate.exchange(url,
                    HttpMethod.valueOf(method), entity, String.class);

            // 处理响应
            Map<String, Object> outputData = new HashMap<>(inputData);
            outputData.put("httpResponse", response.getBody());
            outputData.put("httpStatus", response.getStatusCodeValue());

            return NodeExecutionResult.success(outputData);

        } catch (Exception e) {
            return NodeExecutionResult.error("HTTP请求失败: " + e.getMessage());
        }
    }

    private String buildRequestBody(Map<String, Object> nodeData, Map<String, Object> inputData) {
        // 实现模板变量替换逻辑
        String template = (String)nodeData.get("requestBody") ;
        if (template == null) {
            return "";
        }

        // 简单的变量替换：${variableName}
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            template = template.replace(placeholder,
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }

        return template;
    }

    @Override
    public String getSupportedType() {
        return "http";
    }
}
