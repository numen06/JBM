package com.jbm.cluster.common.mysql.service.openapi;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 过滤并组装对外发布的 OpenAPI spec。
 */
@Component
public class OpenApiSpecSanitizer {

    public String buildPublishedSpec(String title, String version, List<OpenApiOperation> operations) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", StrUtil.blankToDefault(title, "JBM Published API"));
        info.put("version", StrUtil.blankToDefault(version, "1.0.0"));
        spec.put("info", info);
        Map<String, Object> paths = new LinkedHashMap<>();
        for (OpenApiOperation op : operations) {
            if (op == null || StrUtil.isBlank(op.getPath()) || StrUtil.isBlank(op.getRequestMethod())) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(op.getPath(), k -> new LinkedHashMap<>());
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("summary", op.getSummary());
            operation.put("description", op.getDescription());
            if (StrUtil.isNotBlank(op.getTags())) {
                try {
                    operation.put("tags", JSON.parseArray(op.getTags()));
                } catch (Exception ignored) {
                    operation.put("tags", op.getTags());
                }
            }
            if (StrUtil.isNotBlank(op.getParametersJson())) {
                operation.put("parameters", JSON.parse(op.getParametersJson()));
            }
            if (StrUtil.isNotBlank(op.getRequestBodyJson())) {
                operation.put("requestBody", JSON.parse(op.getRequestBodyJson()));
            }
            if (StrUtil.isNotBlank(op.getResponsesJson())) {
                operation.put("responses", JSON.parse(op.getResponsesJson()));
            }
            pathItem.put(op.getRequestMethod().toLowerCase(), operation);
        }
        spec.put("paths", paths);
        return JSON.toJSONString(spec, true);
    }

    public List<OpenApiOperation> filterPublishable(List<OpenApiOperation> operations) {
        List<OpenApiOperation> result = new ArrayList<>();
        for (OpenApiOperation op : operations) {
            if (op == null) {
                continue;
            }
            if (!Integer.valueOf(1).equals(op.getStatus())) {
                continue;
            }
            if (!Integer.valueOf(1).equals(op.getIsOpen())) {
                continue;
            }
            if (op.getApiId() == null) {
                continue;
            }
            if ("MISSING".equalsIgnoreCase(op.getSyncState())) {
                continue;
            }
            result.add(op);
        }
        return result;
    }
}
