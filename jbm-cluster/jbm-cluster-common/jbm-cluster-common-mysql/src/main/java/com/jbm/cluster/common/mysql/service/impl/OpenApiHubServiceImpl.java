package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.OpenApiDocument;
import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.form.OpenApiOperationForm;
import com.jbm.cluster.api.model.api.OpenApiExportRequest;
import com.jbm.cluster.api.model.api.OpenApiSource;
import com.jbm.cluster.api.model.api.OpenApiSyncRequest;
import com.jbm.cluster.api.model.api.OpenApiSyncResult;
import com.jbm.cluster.api.model.api.OpenApiTestRequest;
import com.jbm.cluster.api.model.api.OpenApiTestResult;
import com.jbm.cluster.common.mysql.mapper.OpenApiDocumentMapper;
import com.jbm.cluster.common.mysql.mapper.OpenApiOperationMapper;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.common.mysql.service.OpenApiDocumentService;
import com.jbm.cluster.common.mysql.service.OpenApiHubService;
import com.jbm.cluster.common.mysql.service.OpenApiOperationService;
import com.jbm.cluster.common.mysql.service.openapi.OpenApiHubSupport;
import com.jbm.cluster.common.mysql.service.openapi.OpenApiRouteAliasSupport;
import com.jbm.cluster.common.mysql.service.openapi.OpenApiTestProxyService;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class OpenApiHubServiceImpl implements OpenApiHubService {

    private static final String PLATFORM_PREFIX = "jbm-cluster-platform-";
    private static final Set<String> HTTP_METHODS = CollUtil.newHashSet(
            "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    @Value("${jbm.openapi.gateway-base-url:http://127.0.0.1:6060}")
    private String gatewayBaseUrl;

    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private OpenApiDocumentService openApiDocumentService;
    @Autowired
    private OpenApiDocumentMapper openApiDocumentMapper;
    @Autowired
    private OpenApiOperationService openApiOperationService;
    @Autowired
    private OpenApiOperationMapper openApiOperationMapper;
    @Autowired
    private BaseApiService baseApiService;
    @Autowired
    private OpenApiRouteAliasSupport routeAliasSupport;
    @Autowired
    private OpenApiTestProxyService openApiTestProxyService;

    private final RestTemplate restTemplate = new RestTemplate(requestFactory());

    @Override
    public List<OpenApiSource> listSources() {
        List<OpenApiSource> sources = new ArrayList<>();
        Set<String> discoveredServiceIds = discoveredServiceIds();
        Set<String> serviceIds = collectServiceIds(null);
        for (String serviceId : serviceIds) {
            OpenApiDocument doc = openApiDocumentService.getByServiceId(serviceId);
            OpenApiSource source = new OpenApiSource();
            source.setServiceId(serviceId);
            source.setTitle(doc != null ? doc.getTitle() : serviceId);
            source.setUrl("/api-docs/spec/" + serviceId);
            if (doc != null) {
                source.setSyncStatus(doc.getSyncStatus());
                source.setSyncMessage(doc.getSyncMessage());
                source.setLastSyncTime(doc.getSyncTime());
            } else {
                source.setSyncStatus("PENDING");
            }
            int total = openApiOperationService.countByServiceId(serviceId, null);
            int linked = openApiOperationService.countLinkedByServiceId(serviceId);
            source.setOperationTotal(total);
            source.setLinkedApiTotal(linked);
            source.setUnlinkedApiTotal(Math.max(0, total - linked));
            sources.add(source);
        }
        sources.sort((a, b) -> {
            int rank = Integer.compare(sourceRank(a, discoveredServiceIds), sourceRank(b, discoveredServiceIds));
            if (rank != 0) {
                return rank;
            }
            return StrUtil.nullToEmpty(a.getServiceId()).compareToIgnoreCase(b.getServiceId());
        });
        return sources;
    }

    private int sourceRank(OpenApiSource source, Set<String> discoveredServiceIds) {
        String serviceId = source != null ? source.getServiceId() : "";
        if (source != null && source.getOperationTotal() != null && source.getOperationTotal() > 0) {
            return 0;
        }
        if (discoveredServiceIds.contains(serviceId)) {
            return 1;
        }
        if (source != null && StrUtil.equals("SUCCESS", source.getSyncStatus())) {
            return 2;
        }
        return 3;
    }

    @Override
    public String getRawSpec(String serviceId) {
        OpenApiDocument doc = openApiDocumentService.getByServiceId(serviceId);
        if (doc == null || StrUtil.isBlank(doc.getRawSpec())) {
            throw new ServiceException("尚未同步该服务的 OpenAPI 文档");
        }
        return doc.getRawSpec();
    }

    @Override
    public OpenApiOperation getOperationDetail(Long operationId) {
        OpenApiOperation op = openApiOperationService.getById(operationId);
        if (op == null) {
            throw new ServiceException("接口不存在");
        }
        return op;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OpenApiSyncResult> sync(OpenApiSyncRequest request) {
        List<String> targets = new ArrayList<>(collectServiceIds(
                request != null ? request.getServiceIds() : null));
        List<OpenApiSyncResult> results = new ArrayList<>();
        for (String serviceId : targets) {
            results.add(syncOneService(serviceId));
        }
        return results;
    }

    @Override
    public void export(OpenApiExportRequest request, HttpServletResponse response) {
        List<OpenApiOperation> operations = resolveOperationsForExport(request);
        if (operations.isEmpty()) {
            throw new ServiceException("没有可导出的接口");
        }
        String format = request != null && StrUtil.isNotBlank(request.getFormat())
                ? request.getFormat().toUpperCase() : "JSON";
        try {
            switch (format) {
                case "MARKDOWN":
                case "MD":
                    writeMarkdown(operations, response);
                    break;
                case "HTML":
                    writeHtml(operations, response);
                    break;
                case "YAML":
                    writeJson(operations, response, "openapi.yaml", "application/yaml");
                    break;
                default:
                    writeJson(operations, response, "openapi.json", "application/json");
            }
        } catch (IOException e) {
            throw new ServiceException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public OpenApiTestResult test(OpenApiTestRequest request, String authorization) {
        return openApiTestProxyService.execute(request, authorization);
    }

    private OpenApiSyncResult syncOneService(String serviceId) {
        OpenApiSyncResult result = new OpenApiSyncResult();
        result.setServiceId(serviceId);
        Date now = new Date();
        FetchResult fetch = fetchSpec(serviceId);
        OpenApiDocument doc = openApiDocumentService.getByServiceId(serviceId);
        if (doc == null) {
            doc = new OpenApiDocument();
            doc.setServiceId(serviceId);
            doc.setCreateTime(now);
        }
        doc.setUpdateTime(now);
        doc.setSyncTime(now);
        doc.setSourceUrl(fetch.sourceUrl);
        if (!fetch.success) {
            doc.setSyncStatus("FAILED");
            doc.setSyncMessage(StrUtil.maxLength(fetch.errorMessage, 1000));
            saveDocument(doc);
            result.setSyncStatus("FAILED");
            result.setSyncMessage(doc.getSyncMessage());
            result.setSyncTime(now);
            return result;
        }
        try {
            JSONObject root = JSON.parseObject(fetch.rawSpec);
            if (root == null || root.isEmpty()) {
                throw new ServiceException("OpenAPI spec 为空");
            }
            doc.setRawSpec(fetch.rawSpec);
            doc.setSourceHash(OpenApiHubSupport.sha256(fetch.rawSpec));
            doc.setSyncStatus("SUCCESS");
            doc.setSyncMessage(null);
            JSONObject info = root.getJSONObject("info");
            if (info != null) {
                doc.setTitle(info.getString("title"));
                doc.setVersion(info.getString("version"));
            }
            doc.setSpecVersion(StrUtil.blankToDefault(root.getString("openapi"), root.getString("swagger")));
            saveDocument(doc);
            SyncStats stats = upsertOperationsFromJson(serviceId, doc.getDocId(), root, now);
            result.setSyncStatus("SUCCESS");
            result.setSourceHash(doc.getSourceHash());
            result.setOperationTotal(stats.total);
            result.setLinkedApiTotal(stats.linked);
            result.setUnlinkedApiTotal(stats.total - stats.linked);
            result.setSyncTime(now);
        } catch (Exception e) {
            log.warn("OpenAPI parse/sync failed for {}", serviceId, e);
            doc.setSyncStatus("FAILED");
            doc.setSyncMessage(StrUtil.maxLength(e.getMessage(), 1000));
            saveDocument(doc);
            result.setSyncStatus("FAILED");
            result.setSyncMessage(doc.getSyncMessage());
            result.setSyncTime(now);
        }
        return result;
    }

    private SyncStats upsertOperationsFromJson(String serviceId, Long docId, JSONObject root, Date now) {
        Map<String, OpenApiOperation> existingByKey = new HashMap<>();
        for (OpenApiOperation op : openApiOperationService.listByServiceId(serviceId)) {
            existingByKey.put(op.getOperationKey(), op);
        }
        Set<String> seenKeys = new HashSet<>();
        int total = 0;
        int linked = 0;
        JSONObject paths = root.getJSONObject("paths");
        if (paths == null || paths.isEmpty()) {
            markMissing(existingByKey, seenKeys, now);
            return new SyncStats(0, 0);
        }
        for (String pathKey : paths.keySet()) {
            String path = OpenApiHubSupport.normalizePath(pathKey);
            JSONObject pathItem = paths.getJSONObject(pathKey);
            if (pathItem == null) {
                continue;
            }
            for (String methodKey : pathItem.keySet()) {
                if (methodKey.startsWith("x-") || pathItem.get(methodKey) == null) {
                    continue;
                }
                String method = methodKey.toUpperCase();
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }
                JSONObject operation = pathItem.getJSONObject(methodKey);
                if (operation == null) {
                    continue;
                }
                total++;
                String operationKey = OpenApiHubSupport.operationKey(serviceId, method, path);
                seenKeys.add(operationKey);
                OpenApiOperation entity = existingByKey.get(operationKey);
                boolean isNew = entity == null;
                if (isNew) {
                    entity = new OpenApiOperation();
                    entity.setOperationKey(operationKey);
                    entity.setServiceId(serviceId);
                    entity.setPath(path);
                    entity.setRequestMethod(method);
                    entity.setFirstSeenTime(now);
                    entity.setCreateTime(now);
                }
                entity.setDocId(docId);
                entity.setSummary(operation.getString("summary"));
                entity.setDescription(operation.getString("description"));
                entity.setTags(operation.getJSONArray("tags") != null
                        ? operation.getJSONArray("tags").toJSONString() : null);
                entity.setDeprecated(Boolean.TRUE.equals(operation.getBoolean("deprecated")) ? 1 : 0);
                entity.setParametersJson(operation.getJSONArray("parameters") != null
                        ? operation.getJSONArray("parameters").toJSONString() : null);
                if (operation.containsKey("requestBody")) {
                    entity.setRequestBodyJson(operation.getJSONObject("requestBody").toJSONString());
                } else if (operation.containsKey("body")) {
                    entity.setRequestBodyJson(operation.getJSONObject("body").toJSONString());
                }
                entity.setResponsesJson(operation.getJSONObject("responses") != null
                        ? operation.getJSONObject("responses").toJSONString() : null);
                entity.setSecurityJson(operation.getJSONArray("security") != null
                        ? operation.getJSONArray("security").toJSONString() : null);
                entity.setRawOperationJson(operation.toJSONString());
                String opHash = OpenApiHubSupport.sha256(entity.getRawOperationJson());
                String previousHash = entity.getSourceHash();
                entity.setSourceHash(opHash);
                entity.setLastSeenTime(now);
                entity.setRemovedTime(null);
                entity.setSyncTime(now);
                entity.setUpdateTime(now);
                if (isNew) {
                    entity.setSyncState("NEW");
                    entity.setChangeType("NEW");
                } else if (!StrUtil.equals(previousHash, opHash)) {
                    entity.setSyncState("CHANGED");
                    entity.setChangeType("CHANGED");
                } else {
                    entity.setSyncState("ACTIVE");
                    entity.setChangeType("ACTIVE");
                }
                linkBaseApi(entity);
                if (entity.getApiId() != null) {
                    linked++;
                }
                if (isNew) {
                    openApiOperationMapper.insert(entity);
                } else {
                    openApiOperationMapper.updateById(entity);
                }
            }
        }
        markMissing(existingByKey, seenKeys, now);
        return new SyncStats(total, linked);
    }

    private void markMissing(Map<String, OpenApiOperation> existingByKey, Set<String> seenKeys, Date now) {
        for (Map.Entry<String, OpenApiOperation> entry : existingByKey.entrySet()) {
            if (seenKeys.contains(entry.getKey())) {
                continue;
            }
            OpenApiOperation op = entry.getValue();
            if ("MISSING".equals(op.getSyncState())) {
                continue;
            }
            op.setSyncState("MISSING");
            op.setChangeType("MISSING");
            op.setRemovedTime(now);
            op.setUpdateTime(now);
            openApiOperationMapper.updateById(op);
        }
    }

    private void linkBaseApi(OpenApiOperation entity) {
        BaseApi api = baseApiService.findApiByServicePathMethod(
                entity.getServiceId(), entity.getPath(), entity.getRequestMethod());
        if (api != null) {
            entity.setApiId(api.getApiId());
            entity.setIsOpen(api.getIsOpen());
            entity.setIsAuth(Boolean.TRUE.equals(api.getIsAuth()) ? 1 : 0);
            entity.setStatus(api.getStatus());
        }
    }

    private void saveDocument(OpenApiDocument doc) {
        if (doc.getDocId() == null) {
            openApiDocumentMapper.insert(doc);
        } else {
            openApiDocumentMapper.updateById(doc);
        }
    }

    private FetchResult fetchSpec(String serviceId) {
        String alias = routeAliasSupport.routeAliasFor(serviceId);
        String base = StrUtil.removeSuffix(gatewayBaseUrl, "/");
        List<String> candidates = new ArrayList<>();
        addDiscoverySpecCandidates(serviceId, candidates);
        addCandidate(candidates, base + "/" + alias + "/v2/api-docs");
        addCandidate(candidates, base + "/" + alias + "/v3/api-docs");
        addCandidate(candidates, base + "/" + serviceId + "/v2/api-docs");
        for (String url : candidates) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful()
                        && StrUtil.isNotBlank(response.getBody())) {
                    FetchResult result = new FetchResult();
                    result.success = true;
                    result.rawSpec = response.getBody();
                    result.sourceUrl = url;
                    return result;
                }
            } catch (Exception e) {
                log.debug("Fetch OpenAPI spec failed from {}: {}", url, e.getMessage());
            }
        }
        FetchResult result = new FetchResult();
        result.success = false;
        result.errorMessage = "无法从 Gateway 拉取 " + serviceId + " 的 OpenAPI spec";
        result.sourceUrl = candidates.isEmpty() ? null : candidates.get(0);
        return result;
    }

    private void addDiscoverySpecCandidates(String serviceId, List<String> candidates) {
        List<ServiceInstance> instances = getInstances(serviceId);
        if (CollUtil.isEmpty(instances)) {
            String alias = routeAliasSupport.routeAliasFor(serviceId);
            for (String discoveredServiceId : discoveredServiceIds()) {
                if (StrUtil.equals(discoveredServiceId, serviceId)) {
                    continue;
                }
                if (!StrUtil.equals(alias, routeAliasSupport.routeAliasFor(discoveredServiceId))) {
                    continue;
                }
                instances = getInstances(discoveredServiceId);
                if (CollUtil.isNotEmpty(instances)) {
                    break;
                }
            }
        }
        addInstanceSpecCandidates(instances, candidates);
    }

    private List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> instances;
        try {
            instances = discoveryClient.getInstances(serviceId);
        } catch (Exception e) {
            log.debug("Discovery instances unavailable for {}: {}", serviceId, e.getMessage());
            return Collections.emptyList();
        }
        return instances != null ? instances : Collections.emptyList();
    }

    private void addInstanceSpecCandidates(List<ServiceInstance> instances, List<String> candidates) {
        if (CollUtil.isEmpty(instances)) {
            return;
        }
        Set<String> localHosts = localHostNames();
        instances.sort((a, b) -> Boolean.compare(!isLocalInstance(a, localHosts), !isLocalInstance(b, localHosts)));
        for (ServiceInstance instance : instances) {
            String root = StrUtil.removeSuffix(instance.getUri().toString(), "/");
            addCandidate(candidates, root + "/v2/api-docs");
            addCandidate(candidates, root + "/v3/api-docs");
        }
    }

    private boolean isLocalInstance(ServiceInstance instance, Set<String> localHosts) {
        return instance != null && localHosts.contains(instance.getHost());
    }

    private Set<String> localHostNames() {
        Set<String> hosts = new LinkedHashSet<>();
        hosts.add("127.0.0.1");
        hosts.add("localhost");
        try {
            hosts.add(InetAddress.getLocalHost().getHostAddress());
            hosts.add(InetAddress.getLocalHost().getHostName());
        } catch (Exception ignored) {
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    hosts.add(address.getHostAddress());
                    hosts.add(address.getHostName());
                }
            }
        } catch (SocketException ignored) {
        }
        return Collections.unmodifiableSet(hosts);
    }

    private void addCandidate(List<String> candidates, String url) {
        if (StrUtil.isNotBlank(url) && !candidates.contains(url)) {
            candidates.add(url);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        return factory;
    }

    private Set<String> collectServiceIds(List<String> requested) {
        Set<String> serviceIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(requested)) {
            serviceIds.addAll(requested);
            return serviceIds;
        }
        serviceIds.addAll(discoveredServiceIds());
        for (BaseApi api : baseApiService.findAllList(null)) {
            if (StrUtil.isNotBlank(api.getServiceId())) {
                serviceIds.add(api.getServiceId());
            }
        }
        QueryWrapper<OpenApiDocument> wrapper = new QueryWrapper<>();
        for (OpenApiDocument doc : openApiDocumentMapper.selectList(wrapper)) {
            if (StrUtil.isNotBlank(doc.getServiceId())) {
                serviceIds.add(doc.getServiceId());
            }
        }
        return serviceIds;
    }

    private Set<String> discoveredServiceIds() {
        Set<String> serviceIds = new LinkedHashSet<>();
        try {
            List<String> discovered = discoveryClient.getServices();
            if (discovered != null) {
                for (String serviceId : discovered) {
                    if (StrUtil.startWith(serviceId, PLATFORM_PREFIX)) {
                        serviceIds.add(serviceId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("DiscoveryClient unavailable: {}", e.getMessage());
        }
        return serviceIds;
    }

    private List<OpenApiOperation> resolveOperationsForExport(OpenApiExportRequest request) {
        if (request != null && CollUtil.isNotEmpty(request.getOperationIds())) {
            List<OpenApiOperation> list = new ArrayList<>();
            for (Long id : request.getOperationIds()) {
                OpenApiOperation op = openApiOperationService.getById(id);
                if (op != null) {
                    list.add(op);
                }
            }
            return list;
        }
        OpenApiOperationForm form = new OpenApiOperationForm();
        if (request != null) {
            if (CollUtil.isNotEmpty(request.getServiceIds())) {
                form.setServiceId(request.getServiceIds().get(0));
            }
            if (request.getFilters() != null) {
                Object method = request.getFilters().get("method");
                if (method != null) {
                    form.setMethod(String.valueOf(method));
                }
                Object isOpen = request.getFilters().get("isOpen");
                if (isOpen != null) {
                    form.setIsOpen(Integer.valueOf(String.valueOf(isOpen)));
                }
                Object syncState = request.getFilters().get("syncState");
                if (syncState != null) {
                    form.setSyncState(String.valueOf(syncState));
                }
            }
        }
        com.jbm.framework.usage.paging.PageForm pageForm = new com.jbm.framework.usage.paging.PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(10000);
        form.setPageForm(pageForm);
        return openApiOperationService.findOperationViews(form).getContents().stream()
                .map(v -> openApiOperationService.getById(v.getOperationId()))
                .filter(op -> op != null)
                .collect(java.util.stream.Collectors.toList());
    }

    private void writeJson(List<OpenApiOperation> operations, HttpServletResponse response,
                           String filename, String contentType) throws IOException {
        Map<String, Object> spec = buildExportSpec(operations);
        byte[] bytes = JSON.toJSONString(spec, true).getBytes(StandardCharsets.UTF_8);
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(bytes);
    }

    private void writeMarkdown(List<OpenApiOperation> operations, HttpServletResponse response) throws IOException {
        StringBuilder md = new StringBuilder("# JBM API Export\n\n");
        for (OpenApiOperation op : operations) {
            md.append("## ").append(op.getRequestMethod()).append(' ').append(op.getPath()).append("\n\n");
            if (StrUtil.isNotBlank(op.getSummary())) {
                md.append(op.getSummary()).append("\n\n");
            }
            if (StrUtil.isNotBlank(op.getDescription())) {
                md.append(op.getDescription()).append("\n\n");
            }
            md.append("- 开放: ").append(op.getIsOpen()).append("\n");
            md.append("- 认证: ").append(op.getIsAuth()).append("\n");
            md.append("- 状态: ").append(op.getStatus()).append("\n\n");
        }
        response.setContentType("text/markdown; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"openapi.md\"");
        response.getOutputStream().write(md.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeHtml(List<OpenApiOperation> operations, HttpServletResponse response) throws IOException {
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<title>JBM API Export</title></head><body><h1>JBM API Export</h1>");
        for (OpenApiOperation op : operations) {
            html.append("<h2>").append(op.getRequestMethod()).append(' ').append(op.getPath()).append("</h2>");
            if (StrUtil.isNotBlank(op.getSummary())) {
                html.append("<p>").append(op.getSummary()).append("</p>");
            }
        }
        html.append("</body></html>");
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"openapi.html\"");
        response.getOutputStream().write(html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> buildExportSpec(List<OpenApiOperation> operations) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "JBM API Export");
        info.put("version", "1.0.0");
        spec.put("info", info);
        Map<String, Object> paths = new LinkedHashMap<>();
        for (OpenApiOperation op : operations) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(
                    op.getPath(), k -> new LinkedHashMap<>());
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("summary", op.getSummary());
            operation.put("description", op.getDescription());
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
        return spec;
    }

    private static class FetchResult {
        private boolean success;
        private String rawSpec;
        private String sourceUrl;
        private String errorMessage;
    }

    private static class SyncStats {
        private final int total;
        private final int linked;

        private SyncStats(int total, int linked) {
            this.total = total;
            this.linked = linked;
        }
    }
}
