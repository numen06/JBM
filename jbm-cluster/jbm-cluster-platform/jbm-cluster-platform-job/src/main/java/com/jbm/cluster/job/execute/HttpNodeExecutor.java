package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author scolin
 * @description HTTP节点执行器
 * @date 2025/10/22 11:36
 */
@Component
@Slf4j
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

            // token认证
            if (nodeData.get("token") != null) {
                // 前端有传入就使用输入的token
                headers.set("Authorization", nodeData.get("token").toString());
            } else {
                // 使用系统操作人的token
                headers.set("Authorization", "Bearer " + SecurityUtils.getToken());
            }

            // 替换模板变量
            // String requestBody = buildRequestBody(nodeData, inputData);
            String requestBody = (String) nodeData.get("requestBody");

            // 如果requestBody为空，尝试从requestParams构建
            if (requestBody == null || requestBody.trim().isEmpty()) {
                requestBody = buildRequestBodyFromParams(nodeData);
            }
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 执行HTTP请求
            ResponseEntity<String> response = restTemplate.exchange(url,
                    HttpMethod.valueOf(method), entity, String.class);

            // 检查HTTP状态码
            if (response.getStatusCodeValue() != 200) {
                return NodeExecutionResult.error("HTTP请求失败，状态码: " + response.getStatusCodeValue() +
                        "，响应体: " + response.getBody());
            }

            // 解析响应体，检查业务code
            String responseBody = response.getBody();
            Map<String, Object> outputData = new HashMap<>(inputData);
            outputData.put("httpResponse", responseBody);
            outputData.put("httpStatus", response.getStatusCodeValue());

            // 尝试解析响应体为JSON，检查业务code
            try {
                if (responseBody != null && !responseBody.isEmpty()) {
                    Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);

                    // 检查业务code字段（通常为"code", "status", "resultCode"等）
                    Object businessCode = responseMap.get("code");
                    if (businessCode == null) {
                        businessCode = responseMap.get("status");
                    }
                    if (businessCode == null) {
                        businessCode = responseMap.get("resultCode");
                    }

                    // 如果业务code不是成功状态（通常成功为0, 200, 1000等），则视为失败
                    if (businessCode != null) {
                        String codeStr = businessCode.toString();
                        // 判断业务code是否为成功状态
                        if (!isBusinessCodeSuccess(codeStr)) {
                            Object message = responseMap.get("message");
                            return NodeExecutionResult.error("业务处理失败:" + responseBody);
                        }
                    }

                    outputData.put("responseBody", responseMap);
                }
            } catch (Exception e) {
                // 如果响应体不是JSON，直接返回原始内容
                log.warn("响应体解析为JSON失败，将使用原始内容", e);
            }

            return NodeExecutionResult.success(outputData);

        } catch (Exception e) {
            return NodeExecutionResult.error("HTTP请求失败: " + e.getMessage());
        }
    }

    /**
     * 判断业务code是否为成功状态
     * 只有200算是成功
     */
    private boolean isBusinessCodeSuccess(String code) {
        if (code == null) {
            return false;
        }

        // 只有200算是成功
        try {
            int codeInt = Integer.parseInt(code);
            return codeInt == 200;
        } catch (NumberFormatException e) {
            // 非数字型code
        }

        // 字符串型成功标识
        String codeLower = code.toLowerCase();
        return codeLower.equals("success") || codeLower.equals("ok");
    }

    private String buildRequestBody(Map<String, Object> nodeData, Map<String, Object> inputData) {
        // 实现模板变量替换逻辑
        String template = (String) nodeData.get("requestBody");
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

    /**
     * 从requestParams构建POST请求体
     * 当前端还没有实现requestBody时，从requestParams数组中的name和value组成JSON
     */
    private String buildRequestBodyFromParams(Map<String, Object> nodeData) {
        Object requestParamsObj = nodeData.get("requestParams");
        if (requestParamsObj == null) {
            return "{}";
        }

        try {
            List<Map<String, Object>> requestParams = (List<Map<String, Object>>) requestParamsObj;
            Map<String, Object> bodyMap = new HashMap<>();

            // 遍历requestParams，从name和value构建请求体
            for (Map<String, Object> param : requestParams) {
                String name = (String) param.get("name");
                Object value = param.get("value");

                if (name != null && !name.trim().isEmpty()) {
                    // 尝试转换value的类型
                    Object convertedValue = convertParamValue(value, (String) param.get("type"));
                    bodyMap.put(name, convertedValue);
                }
            }

            // 转换为JSON字符串
            return JSONUtil.toJsonStr(bodyMap);
        } catch (Exception e) {
            log.warn("从requestParams构建请求体失败，将使用空JSON", e);
            return "{}";
        }
    }

    /**
     * 转换参数值的类型
     */
    private Object convertParamValue(Object value, String type) {
        if (value == null) {
            return null;
        }

        if ("Number".equalsIgnoreCase(type) || "Integer".equalsIgnoreCase(type)) {
            try {
                if (value instanceof Number) {
                    return value;
                }
                String valueStr = value.toString();
                if (valueStr.contains(".")) {
                    return Double.parseDouble(valueStr);
                } else {
                    return Long.parseLong(valueStr);
                }
            } catch (Exception e) {
                return value;
            }
        } else if ("Boolean".equalsIgnoreCase(type)) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        } else if ("Array".equalsIgnoreCase(type)) {
            if (value instanceof List) {
                return value;
            }
            // 尝试解析为JSON数组
            try {
                return JSONUtil.parseArray(value.toString());
            } catch (Exception e) {
                return value;
            }
        } else {
            // 默认作为字符串
            return value.toString();
        }
    }

    @Override
    public String getSupportedType() {
        return "http";
    }
}
