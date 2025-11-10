package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.core.constant.QueueConstants;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * API资源扫描 - 通过OpenAPI文档
 *
 * @author wesley.zhang
 */
@Slf4j
public class JbmApiResourceScan extends JbmClusterResourceScan<JbmApiResource> {

    private static final String DEFAULT_OPENAPI_PATH = "/v2/api-docs";
    private static final int MAX_RETRY_TIMES = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    @Override
    public String queue() {
        return QueueConstants.API_RESOURCE_STREAM;
    }

    @Override
    public boolean enable(JbmClusterProperties jbmClusterProperties) {
        return BooleanUtil.isTrue(jbmClusterProperties.getApiRegister());
    }

    @Override
    public JbmApiResource scan() {
        String serviceId = SpringContextHolder.geteApplicationName();
        List<JbmApi> jbmApis = Lists.newArrayList();

        try {
            Environment environment = SpringContextHolder.getBean(Environment.class);
            
            // 检查是否为Web应用
            if (!isWebApplication(environment)) {
                log.info("非Web应用，跳过API资源扫描");
                JbmApiResource jbmApiResource = new JbmApiResource();
                jbmApiResource.setServiceId(serviceId);
                jbmApiResource.setJbmApiList(jbmApis);
                return jbmApiResource;
            }
            
            // 获取本地服务端口和上下文路径
            String port = environment.getProperty("server.port", "8080");
            String contextPath = environment.getProperty("server.servlet.context-path", "");
            
            // 构建OpenAPI文档地址
            String openApiUrl = String.format("http://127.0.0.1:%s%s%s", port, contextPath, DEFAULT_OPENAPI_PATH);
            log.info("开始从OpenAPI文档获取API信息: {}", openApiUrl);

            // 获取OpenAPI文档（带重试）
            String openApiJson = fetchOpenApiDocWithRetry(openApiUrl);
            
            if (openApiJson != null) {
                // 解析OpenAPI文档
                jbmApis = parseOpenApiDoc(openApiJson, serviceId);
                log.info("从OpenAPI文档解析到 {} 个API", jbmApis.size());
            } else {
                log.warn("无法获取OpenAPI文档，API资源为空");
            }
        } catch (Exception e) {
            log.error("扫描OpenAPI文档失败", e);
        }

        JbmApiResource jbmApiResource = new JbmApiResource();
        jbmApiResource.setServiceId(serviceId);
        jbmApiResource.setJbmApiList(jbmApis);
        return jbmApiResource;
    }

    /**
     * 判断是否为Web应用
     */
    private boolean isWebApplication(Environment environment) {
        try {
            // 检查是否存在server.port配置
            String port = environment.getProperty("server.port");
            if (port == null) {
                return false;
            }
            
            // 检查是否存在Web相关的Bean
            return SpringContextHolder.getApplicationContext().containsBean("requestMappingHandlerMapping");
        } catch (Exception e) {
            log.debug("检查Web应用类型失败", e);
            return false;
        }
    }

    /**
     * 带重试的获取OpenAPI文档
     */
    private String fetchOpenApiDocWithRetry(String url) {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            if (i > 0) {
                log.info("第 {} 次重试获取OpenAPI文档...", i);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("重试等待被中断");
                    break;
                }
            }
            
            String result = fetchOpenApiDoc(url);
            if (result != null) {
                return result;
            }
        }
        
        log.error("经过 {} 次尝试后仍无法获取OpenAPI文档", MAX_RETRY_TIMES);
        return null;
    }

    /**
     * 获取OpenAPI文档
     */
    private String fetchOpenApiDoc(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    if (body != null && !body.isEmpty()) {
                        log.debug("成功获取OpenAPI文档，大小: {} bytes", body.length());
                        return body;
                    }
                } else {
                    log.debug("获取OpenAPI文档失败，状态码: {}", response.code());
                }
            }
        } catch (java.net.ConnectException e) {
            log.debug("连接OpenAPI接口失败，服务可能还未完全启动: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("请求OpenAPI文档异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析OpenAPI文档
     */
    private List<JbmApi> parseOpenApiDoc(String openApiJson, String serviceId) {
        List<JbmApi> jbmApis = Lists.newArrayList();
        
        try {
            JSONObject apiDoc = JSONUtil.parseObj(openApiJson);
            JSONObject paths = apiDoc.getJSONObject("paths");
            
            if (paths == null || paths.isEmpty()) {
                return jbmApis;
            }

            // 遍历所有路径
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                JSONObject pathItem = (JSONObject) pathEntry.getValue();

                // 遍历该路径下的所有HTTP方法
                for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                    String method = methodEntry.getKey().toUpperCase();
                    
                    // 过滤非HTTP方法的字段
                    if (!isHttpMethod(method)) {
                        continue;
                    }

                    JSONObject operation = (JSONObject) methodEntry.getValue();
                    
                    // 检查是否被忽略
                    JSONArray tags = operation.getJSONArray("tags");
                    if (tags != null && tags.contains("ApiIgnore")) {
                        continue;
                    }

                    JbmApi jbmApi = buildJbmApiFromOperation(path, method, operation, serviceId);
                    if (jbmApi != null) {
                        jbmApis.add(jbmApi);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析OpenAPI文档失败", e);
        }

        return jbmApis;
    }

    /**
     * 从Operation构建JbmApi
     */
    private JbmApi buildJbmApiFromOperation(String path, String method, JSONObject operation, String serviceId) {
        try {
            // 生成唯一标识
            String md5 = DigestUtil.md5Hex(serviceId + path);
            
            // 获取API名称和描述
            String apiName = operation.getStr("summary", "");
            String apiDesc = operation.getStr("description", "");
            
            // 获取operationId，通常格式为 "ControllerName_methodName"
            String operationId = operation.getStr("operationId", "");
            String methodName = extractMethodName(operationId);
            String className = extractClassName(operationId);

            // 获取请求方法
            Set<String> requestMethods = Sets.newHashSet(method);
            
            // 获取Content-Type
            Set<String> contentTypes = Sets.newHashSet();
            JSONArray consumes = operation.getJSONArray("consumes");
            if (consumes != null) {
                for (Object consume : consumes) {
                    contentTypes.add(consume.toString());
                }
            }
            JSONArray produces = operation.getJSONArray("produces");
            if (produces != null) {
                for (Object produce : produces) {
                    contentTypes.add(produce.toString());
                }
            }
            
            if (contentTypes.isEmpty()) {
                contentTypes.add("application/json");
            }

            // 构建JbmApi
            return JbmApi.builder()
                    .apiName(apiName)
                    .apiCode(md5)
                    .apiDesc(apiDesc)
                    .paths(Sets.newHashSet(path))
                    .className(className)
                    .methodName(methodName)
                    .requestMethods(requestMethods)
                    .md5(md5)
                    .serviceId(serviceId)
                    .contentTypes(contentTypes)
                    .isAuth(true)
                    .accessLog(true)
                    .build();
        } catch (Exception e) {
            log.error("构建JbmApi失败，path: {}, method: {}", path, method, e);
            return null;
        }
    }

    /**
     * 从operationId提取方法名
     */
    private String extractMethodName(String operationId) {
        if (operationId == null || operationId.isEmpty()) {
            return "";
        }
        // operationId格式通常为: methodNameUsingGET、methodNameUsingPOST等
        // 或者: ClassName_methodName
        if (operationId.contains("Using")) {
            return operationId.substring(0, operationId.indexOf("Using"));
        }
        if (operationId.contains("_")) {
            return operationId.substring(operationId.lastIndexOf("_") + 1);
        }
        return operationId;
    }

    /**
     * 从operationId提取类名
     */
    private String extractClassName(String operationId) {
        if (operationId == null || operationId.isEmpty()) {
            return "";
        }
        // 尝试从operationId中提取类名
        if (operationId.contains("_")) {
            return operationId.substring(0, operationId.indexOf("_"));
        }
        return "";
    }

    /**
     * 判断是否为HTTP方法
     */
    private boolean isHttpMethod(String method) {
        return "GET".equals(method) || "POST".equals(method) || 
               "PUT".equals(method) || "DELETE".equals(method) || 
               "PATCH".equals(method) || "OPTIONS".equals(method) || 
               "HEAD".equals(method);
    }
}
