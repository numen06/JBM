package com.jbm.cluster.common.mysql.service.openapi;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.model.api.OpenApiTestRequest;
import com.jbm.cluster.api.model.api.OpenApiTestResult;
import com.jbm.cluster.common.mysql.service.OpenApiOperationService;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class OpenApiTestProxyService {

    private static final int BODY_PREVIEW_LIMIT = 8192;
    private static final int TIMEOUT_MS = 10000;
    private static final Set<String> BLOCKED_HEADERS = new HashSet<>();

    static {
        BLOCKED_HEADERS.add("authorization");
        BLOCKED_HEADERS.add("cookie");
        BLOCKED_HEADERS.add("x-jbm-signature");
        BLOCKED_HEADERS.add("secretkey");
        BLOCKED_HEADERS.add("privatekey");
    }

    @Value("${jbm.openapi.gateway-base-url:http://127.0.0.1:6060}")
    private String gatewayBaseUrl;

    @Autowired
    private OpenApiOperationService openApiOperationService;
    @Autowired
    private OpenApiRouteAliasSupport routeAliasSupport;

    private final RestTemplate restTemplate = new RestTemplate(requestFactory());

    public OpenApiTestResult execute(OpenApiTestRequest request, String callerAuthorization) {
        OpenApiOperation operation = resolveOperation(request);
        String method = operation.getRequestMethod().toUpperCase();
        validateMethod(method, request.getConfirm());
        String alias = routeAliasSupport.routeAliasFor(operation.getServiceId());
        String resolvedPath = resolvePath(operation.getPath(), request.getPathParams());
        String url = buildUrl(effectiveGatewayBaseUrl(request), alias, resolvedPath, request.getQueryParams());
        HttpHeaders headers = sanitizeHeaders(request.getHeaders());
        boolean authorizationApplied = applyCallerAuthorization(headers, callerAuthorization);
        applyDefaultContentType(headers, request.getBody());
        HttpEntity<String> entity = new HttpEntity<>(request.getBody(), headers);
        long start = System.currentTimeMillis();
        OpenApiTestResult result = new OpenApiTestResult();
        result.setTarget(method + " /" + alias + resolvedPath);
        result.setRequestUrl(url);
        result.setAuthorizationApplied(authorizationApplied);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.valueOf(method), entity, String.class);
            result.setSuccess(true);
            result.setStatus(response.getStatusCodeValue());
            result.setHeaders(flattenHeaders(response.getHeaders()));
            applyBodyPreview(result, response.getBody());
        } catch (HttpStatusCodeException e) {
            result.setSuccess(false);
            result.setStatus(e.getRawStatusCode());
            result.setHeaders(flattenHeaders(e.getResponseHeaders()));
            result.setErrorType("HTTP_STATUS");
            result.setErrorMessage(StrUtil.maxLength(e.getStatusText(), 500));
            applyBodyPreview(result, e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            result.setSuccess(false);
            result.setErrorType("NETWORK");
            result.setErrorMessage(StrUtil.maxLength(e.getMessage(), 500));
            log.warn("OpenAPI test proxy network failed: {}", result.getTarget(), e);
        } catch (RestClientException e) {
            result.setSuccess(false);
            result.setErrorType("CLIENT");
            result.setErrorMessage(StrUtil.maxLength(e.getMessage(), 500));
            log.warn("OpenAPI test proxy client failed: {}", result.getTarget(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorType("UNKNOWN");
            result.setErrorMessage(StrUtil.maxLength(e.getMessage(), 500));
            log.warn("OpenAPI test proxy failed: {}", result.getTarget(), e);
        } finally {
            result.setDurationMs(System.currentTimeMillis() - start);
        }
        return result;
    }

    private boolean applyCallerAuthorization(HttpHeaders headers, String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return false;
        }
        String value = authorization.trim();
        if (StrUtil.startWithIgnoreCase(value, "Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, value);
            return true;
        }
        return false;
    }

    private void applyDefaultContentType(HttpHeaders headers, String body) {
        if (StrUtil.isNotBlank(body) && headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
    }

    private void applyBodyPreview(OpenApiTestResult result, String body) {
        String preview = body != null ? body : "";
        result.setTruncated(preview.length() > BODY_PREVIEW_LIMIT);
        result.setBodyPreview(Boolean.TRUE.equals(result.getTruncated())
                ? preview.substring(0, BODY_PREVIEW_LIMIT) : preview);
    }

    private OpenApiOperation resolveOperation(OpenApiTestRequest request) {
        if (request.getOperationId() != null) {
            OpenApiOperation op = openApiOperationService.getById(request.getOperationId());
            if (op == null) {
                throw new ServiceException("接口不存在");
            }
            return op;
        }
        if (StrUtil.isNotBlank(request.getServiceId())
                && StrUtil.isNotBlank(request.getPath())
                && StrUtil.isNotBlank(request.getMethod())) {
            String key = OpenApiHubSupport.operationKey(
                    request.getServiceId(), request.getMethod(), request.getPath());
            OpenApiOperation op = openApiOperationService.getByOperationKey(key);
            if (op == null) {
                throw new ServiceException("接口不存在");
            }
            return op;
        }
        throw new ServiceException("operationId 或 serviceId+path+method 必填");
    }

    private void validateMethod(String method, Boolean confirm) {
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }
        if (!Boolean.TRUE.equals(confirm)) {
            throw new ServiceException("写操作测试需要 confirm=true");
        }
    }

    private String effectiveGatewayBaseUrl(OpenApiTestRequest request) {
        if (request != null && StrUtil.isNotBlank(request.getGatewayBaseUrl())) {
            return StrUtil.removeSuffix(request.getGatewayBaseUrl().trim(), "/");
        }
        return StrUtil.removeSuffix(gatewayBaseUrl, "/");
    }

    private String buildUrl(String baseUrl, String alias, String path, Map<String, String> queryParams) {
        String base = StrUtil.removeSuffix(baseUrl, "/");
        StringBuilder url = new StringBuilder(base).append('/').append(alias);
        if (!path.startsWith("/")) {
            url.append('/');
        }
        url.append(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            url.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    url.append('&');
                }
                url.append(UriUtils.encodeQueryParam(StrUtil.nullToEmpty(entry.getKey()), "UTF-8"))
                        .append('=')
                        .append(UriUtils.encodeQueryParam(StrUtil.nullToEmpty(entry.getValue()), "UTF-8"));
                first = false;
            }
        }
        return url.toString();
    }

    private String resolvePath(String template, Map<String, String> pathParams) {
        if (pathParams == null || pathParams.isEmpty()) {
            return template;
        }
        String resolved = template;
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}",
                    UriUtils.encodePathSegment(StrUtil.nullToEmpty(entry.getValue()), "UTF-8"));
        }
        return resolved;
    }

    private HttpHeaders sanitizeHeaders(Map<String, String> headers) {
        HttpHeaders result = new HttpHeaders();
        if (headers == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() == null || BLOCKED_HEADERS.contains(entry.getKey().toLowerCase())) {
                continue;
            }
            result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        if (headers == null) {
            return map;
        }
        headers.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                map.put(k, v.get(0));
            }
        });
        return map;
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }
}
