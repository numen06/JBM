package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import java.text.SimpleDateFormat;
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
        hydrateMissingSchemas(operations);
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
        String schemasJson = extractSchemasJson(root);
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
                entity.setSchemasJson(schemasJson);
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

    private String extractSchemasJson(JSONObject root) {
        JSONObject schemas = new JSONObject(true);
        JSONObject definitions = root.getJSONObject("definitions");
        if (definitions != null) {
            schemas.putAll(definitions);
        }
        JSONObject components = root.getJSONObject("components");
        if (components != null && components.getJSONObject("schemas") != null) {
            schemas.putAll(components.getJSONObject("schemas"));
        }
        return schemas.isEmpty() ? null : schemas.toJSONString();
    }

    private void hydrateMissingSchemas(List<OpenApiOperation> operations) {
        Map<Long, String> schemasByDocId = new HashMap<>();
        for (OpenApiOperation operation : operations) {
            if (operation == null || StrUtil.isNotBlank(operation.getSchemasJson()) || operation.getDocId() == null) {
                continue;
            }
            String schemasJson = schemasByDocId.get(operation.getDocId());
            if (!schemasByDocId.containsKey(operation.getDocId())) {
                schemasJson = null;
                OpenApiDocument doc = openApiDocumentMapper.selectById(operation.getDocId());
                if (doc != null && StrUtil.isNotBlank(doc.getRawSpec())) {
                    try {
                        schemasJson = extractSchemasJson(JSON.parseObject(doc.getRawSpec()));
                    } catch (Exception ignored) {
                    }
                }
                schemasByDocId.put(operation.getDocId(), schemasJson);
            }
            operation.setSchemasJson(schemasJson);
        }
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
        List<OpenApiOperation> sorted = sortedOperations(operations);
        StringBuilder md = new StringBuilder("# JBM OpenAPI 接口文档\n\n");
        md.append("- 生成时间: ").append(nowText()).append("\n");
        md.append("- 接口数量: ").append(sorted.size()).append("\n");
        md.append("- 说明: 本文档由已同步的 Swagger/OpenAPI spec 生成，JSON/YAML 导出仍保留给 Apifox/Postman 导入。\n\n");
        md.append("## 目录\n\n");
        for (OpenApiOperation op : sorted) {
            md.append("- ").append(op.getServiceId()).append(" / ").append(firstTag(op)).append(" / ")
                    .append(op.getRequestMethod()).append(' ').append(op.getPath()).append("\n");
        }
        md.append("\n");

        String currentService = null;
        String currentTag = null;
        for (OpenApiOperation op : sorted) {
            if (!StrUtil.equals(currentService, op.getServiceId())) {
                currentService = op.getServiceId();
                currentTag = null;
                md.append("## ").append(currentService).append("\n\n");
            }
            String tag = firstTag(op);
            if (!StrUtil.equals(currentTag, tag)) {
                currentTag = tag;
                md.append("### ").append(tag).append("\n\n");
            }
            appendMarkdownOperation(md, op);
        }
        response.setContentType("text/markdown; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + exportFilename("md") + "\"");
        response.getOutputStream().write(md.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeHtml(List<OpenApiOperation> operations, HttpServletResponse response) throws IOException {
        List<OpenApiOperation> sorted = sortedOperations(operations);
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>JBM OpenAPI 接口文档</title><style>"
                + "body{margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;color:#172033;background:#f8fafc;}"
                + "aside{position:fixed;left:0;top:0;bottom:0;width:300px;overflow:auto;background:#fff;border-right:1px solid #e5e7eb;padding:20px;box-sizing:border-box;}"
                + "main{margin-left:300px;padding:28px 40px;max-width:1180px;}"
                + "h1{font-size:28px;margin:0 0 8px;}h2{margin-top:34px;border-bottom:1px solid #e5e7eb;padding-bottom:8px;}h3{margin-top:26px;}"
                + ".op{background:#fff;border:1px solid #e5e7eb;border-radius:8px;margin:16px 0;padding:18px;}"
                + ".method{display:inline-block;min-width:58px;text-align:center;border-radius:4px;padding:3px 8px;margin-right:8px;font-weight:700;font-size:12px;background:#dbeafe;color:#1d4ed8;}"
                + ".GET{background:#dcfce7;color:#166534}.POST{background:#dbeafe;color:#1d4ed8}.PUT{background:#fef3c7;color:#92400e}.PATCH{background:#ffedd5;color:#9a3412}.DELETE{background:#fee2e2;color:#991b1b}"
                + "code,pre{font-family:Consolas,'JetBrains Mono',monospace;}pre{background:#0f172a;color:#e2e8f0;border-radius:6px;padding:12px;overflow:auto;}"
                + "table{width:100%;border-collapse:collapse;margin:10px 0 16px;}th,td{border:1px solid #e5e7eb;padding:8px;text-align:left;vertical-align:top;}th{background:#f1f5f9;}"
                + ".meta{color:#64748b;font-size:13px}.toc a{display:block;color:#334155;text-decoration:none;margin:6px 0;font-size:13px}.empty{color:#94a3b8;}"
                + "</style></head><body><aside><h2>目录</h2><div class=\"toc\">");
        int index = 0;
        for (OpenApiOperation op : sorted) {
            html.append("<a href=\"#op-").append(index).append("\">")
                    .append(escapeHtml(op.getServiceId())).append(" / ")
                    .append(escapeHtml(firstTag(op))).append(" / ")
                    .append(escapeHtml(op.getRequestMethod())).append(' ')
                    .append(escapeHtml(op.getPath())).append("</a>");
            index++;
        }
        html.append("</div></aside><main><h1>JBM OpenAPI 接口文档</h1>")
                .append("<p class=\"meta\">生成时间: ").append(escapeHtml(nowText()))
                .append(" · 接口数量: ").append(sorted.size()).append("</p>");
        index = 0;
        String currentService = null;
        String currentTag = null;
        for (OpenApiOperation op : sorted) {
            if (!StrUtil.equals(currentService, op.getServiceId())) {
                currentService = op.getServiceId();
                currentTag = null;
                html.append("<h2>").append(escapeHtml(currentService)).append("</h2>");
            }
            String tag = firstTag(op);
            if (!StrUtil.equals(currentTag, tag)) {
                currentTag = tag;
                html.append("<h3>").append(escapeHtml(tag)).append("</h3>");
            }
            appendHtmlOperation(html, op, index++);
        }
        html.append("</main></body></html>");
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + exportFilename("html") + "\"");
        response.getOutputStream().write(html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendMarkdownOperation(StringBuilder md, OpenApiOperation op) {
        md.append("#### ").append(op.getRequestMethod()).append(' ').append(op.getPath()).append("\n\n");
        md.append(StrUtil.blankToDefault(op.getSummary(), "接口说明待补充")).append("\n\n");
        if (StrUtil.isNotBlank(op.getDescription()) && !StrUtil.equals(op.getSummary(), op.getDescription())) {
            md.append(op.getDescription()).append("\n\n");
        }
        md.append("| 属性 | 值 |\n|---|---|\n");
        md.append("| 服务 | ").append(tableCell(op.getServiceId())).append(" |\n");
        md.append("| 标签 | ").append(tableCell(firstTag(op))).append(" |\n");
        md.append("| 是否开放 | ").append(tableCell(Boolean.TRUE.equals(op.getIsOpen() != null && op.getIsOpen() == 1) ? "是" : "否")).append(" |\n");
        md.append("| 是否认证 | ").append(tableCell(op.getIsAuth() != null && op.getIsAuth() == 1 ? "是" : "否")).append(" |\n");
        md.append("| 治理状态 | ").append(tableCell(text(op.getStatus()))).append(" |\n\n");
        appendMarkdownParameters(md, op, "path", "路径参数");
        appendMarkdownParameters(md, op, "query", "查询参数");
        appendMarkdownParameters(md, op, "header", "请求头");
        appendMarkdownParameters(md, op, "formData", "表单参数");
        appendMarkdownRequestBody(md, op);
        appendMarkdownResponses(md, op);
    }

    private void appendMarkdownParameters(StringBuilder md, OpenApiOperation op, String in, String title) {
        JSONArray params = filterParameters(op, in);
        if (params.isEmpty()) {
            return;
        }
        md.append("**").append(title).append("**\n\n");
        md.append("| 参数 | 类型 | 必填 | 示例 | 说明 |\n|---|---|---|---|---|\n");
        for (Object item : params) {
            JSONObject p = (JSONObject) item;
            md.append("| ").append(tableCell(p.getString("name")))
                    .append(" | ").append(tableCell(parameterType(p)))
                    .append(" | ").append(Boolean.TRUE.equals(p.getBoolean("required")) ? "是" : "否")
                    .append(" | ").append(tableCell(exampleValue(p)))
                    .append(" | ").append(tableCell(p.getString("description")))
                    .append(" |\n");
        }
        md.append("\n");
    }

    private void appendMarkdownRequestBody(StringBuilder md, OpenApiOperation op) {
        JSONObject schema = requestSchema(op);
        if (schema == null) {
            return;
        }
        JSONObject schemas = parseJsonObject(op.getSchemasJson());
        List<SchemaField> fields = schemaFields(schema, schemas);
        md.append("**请求体**\n\n");
        appendMarkdownSchemaTable(md, fields);
        Object sample = sampleFromSchema(schema, schemas, 0);
        md.append("请求示例\n\n```json\n").append(prettyJson(sample)).append("\n```\n\n");
    }

    private void appendMarkdownResponses(StringBuilder md, OpenApiOperation op) {
        JSONObject responses = parseJsonObject(op.getResponsesJson());
        if (responses == null || responses.isEmpty()) {
            return;
        }
        JSONObject schemas = parseJsonObject(op.getSchemasJson());
        md.append("**响应**\n\n");
        for (String status : responses.keySet()) {
            JSONObject response = responses.getJSONObject(status);
            if (response == null) {
                continue;
            }
            md.append("状态码 `").append(status).append("`: ")
                    .append(StrUtil.blankToDefault(response.getString("description"), "-")).append("\n\n");
            JSONObject schema = responseSchema(response);
            if (schema != null) {
                appendMarkdownSchemaTable(md, schemaFields(schema, schemas));
                md.append("响应示例\n\n```json\n").append(prettyJson(sampleFromSchema(schema, schemas, 0))).append("\n```\n\n");
            }
        }
    }

    private void appendMarkdownSchemaTable(StringBuilder md, List<SchemaField> fields) {
        if (fields.isEmpty()) {
            md.append("_结构未在 spec 中声明，详见原始 schema。_\n\n");
            return;
        }
        md.append("| 字段 | 类型 | 必填 | 说明 |\n|---|---|---|---|\n");
        for (SchemaField field : fields) {
            md.append("| ").append(tableCell(field.name))
                    .append(" | ").append(tableCell(field.type))
                    .append(" | ").append(field.required ? "是" : "否")
                    .append(" | ").append(tableCell(field.description))
                    .append(" |\n");
        }
        md.append("\n");
    }

    private void appendHtmlOperation(StringBuilder html, OpenApiOperation op, int index) {
        html.append("<section class=\"op\" id=\"op-").append(index).append("\"><h3><span class=\"method ")
                .append(escapeHtml(op.getRequestMethod())).append("\">")
                .append(escapeHtml(op.getRequestMethod())).append("</span><code>")
                .append(escapeHtml(op.getPath())).append("</code></h3>");
        html.append("<p>").append(escapeHtml(StrUtil.blankToDefault(op.getSummary(), "接口说明待补充"))).append("</p>");
        if (StrUtil.isNotBlank(op.getDescription()) && !StrUtil.equals(op.getSummary(), op.getDescription())) {
            html.append("<p class=\"meta\">").append(escapeHtml(op.getDescription())).append("</p>");
        }
        html.append("<table><tbody>")
                .append(htmlRow("服务", op.getServiceId()))
                .append(htmlRow("标签", firstTag(op)))
                .append(htmlRow("是否开放", op.getIsOpen() != null && op.getIsOpen() == 1 ? "是" : "否"))
                .append(htmlRow("是否认证", op.getIsAuth() != null && op.getIsAuth() == 1 ? "是" : "否"))
                .append(htmlRow("治理状态", text(op.getStatus())))
                .append("</tbody></table>");
        appendHtmlParameters(html, op, "path", "路径参数");
        appendHtmlParameters(html, op, "query", "查询参数");
        appendHtmlParameters(html, op, "header", "请求头");
        appendHtmlParameters(html, op, "formData", "表单参数");
        appendHtmlRequestBody(html, op);
        appendHtmlResponses(html, op);
        html.append("</section>");
    }

    private void appendHtmlParameters(StringBuilder html, OpenApiOperation op, String in, String title) {
        JSONArray params = filterParameters(op, in);
        if (params.isEmpty()) {
            return;
        }
        html.append("<h4>").append(escapeHtml(title)).append("</h4><table><thead><tr>")
                .append("<th>参数</th><th>类型</th><th>必填</th><th>示例</th><th>说明</th>")
                .append("</tr></thead><tbody>");
        for (Object item : params) {
            JSONObject p = (JSONObject) item;
            html.append("<tr><td><code>").append(escapeHtml(p.getString("name"))).append("</code></td>")
                    .append("<td>").append(escapeHtml(parameterType(p))).append("</td>")
                    .append("<td>").append(Boolean.TRUE.equals(p.getBoolean("required")) ? "是" : "否").append("</td>")
                    .append("<td>").append(escapeHtml(exampleValue(p))).append("</td>")
                    .append("<td>").append(escapeHtml(p.getString("description"))).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendHtmlRequestBody(StringBuilder html, OpenApiOperation op) {
        JSONObject schema = requestSchema(op);
        if (schema == null) {
            return;
        }
        JSONObject schemas = parseJsonObject(op.getSchemasJson());
        html.append("<h4>请求体</h4>");
        appendHtmlSchemaTable(html, schemaFields(schema, schemas));
        html.append("<p class=\"meta\">请求示例</p><pre>").append(escapeHtml(prettyJson(sampleFromSchema(schema, schemas, 0)))).append("</pre>");
    }

    private void appendHtmlResponses(StringBuilder html, OpenApiOperation op) {
        JSONObject responses = parseJsonObject(op.getResponsesJson());
        if (responses == null || responses.isEmpty()) {
            return;
        }
        JSONObject schemas = parseJsonObject(op.getSchemasJson());
        html.append("<h4>响应</h4>");
        for (String status : responses.keySet()) {
            JSONObject response = responses.getJSONObject(status);
            if (response == null) {
                continue;
            }
            html.append("<p><strong>").append(escapeHtml(status)).append("</strong> ")
                    .append(escapeHtml(StrUtil.blankToDefault(response.getString("description"), ""))).append("</p>");
            JSONObject schema = responseSchema(response);
            if (schema != null) {
                appendHtmlSchemaTable(html, schemaFields(schema, schemas));
                html.append("<pre>").append(escapeHtml(prettyJson(sampleFromSchema(schema, schemas, 0)))).append("</pre>");
            }
        }
    }

    private void appendHtmlSchemaTable(StringBuilder html, List<SchemaField> fields) {
        if (fields.isEmpty()) {
            html.append("<p class=\"empty\">结构未在 spec 中声明，详见原始 schema。</p>");
            return;
        }
        html.append("<table><thead><tr><th>字段</th><th>类型</th><th>必填</th><th>说明</th></tr></thead><tbody>");
        for (SchemaField field : fields) {
            html.append("<tr><td><code>").append(escapeHtml(field.name)).append("</code></td>")
                    .append("<td>").append(escapeHtml(field.type)).append("</td>")
                    .append("<td>").append(field.required ? "是" : "否").append("</td>")
                    .append("<td>").append(escapeHtml(field.description)).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private List<OpenApiOperation> sortedOperations(List<OpenApiOperation> operations) {
        List<OpenApiOperation> sorted = new ArrayList<>(operations);
        sorted.sort((a, b) -> {
            int service = text(a.getServiceId()).compareToIgnoreCase(text(b.getServiceId()));
            if (service != 0) {
                return service;
            }
            int tag = firstTag(a).compareToIgnoreCase(firstTag(b));
            if (tag != 0) {
                return tag;
            }
            int path = text(a.getPath()).compareToIgnoreCase(text(b.getPath()));
            if (path != 0) {
                return path;
            }
            return text(a.getRequestMethod()).compareToIgnoreCase(text(b.getRequestMethod()));
        });
        return sorted;
    }

    private JSONArray filterParameters(OpenApiOperation op, String in) {
        JSONArray result = new JSONArray();
        JSONArray params = parseJsonArray(op.getParametersJson());
        for (Object item : params) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject param = (JSONObject) item;
            if (StrUtil.equalsIgnoreCase(in, param.getString("in"))) {
                result.add(param);
            }
        }
        return result;
    }

    private JSONObject requestSchema(OpenApiOperation op) {
        JSONObject requestBody = parseJsonObject(op.getRequestBodyJson());
        JSONObject schema = schemaFromBodyObject(requestBody);
        if (schema != null) {
            return schema;
        }
        JSONArray params = parseJsonArray(op.getParametersJson());
        for (Object item : params) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject param = (JSONObject) item;
            if (StrUtil.equalsIgnoreCase("body", param.getString("in")) && param.getJSONObject("schema") != null) {
                return param.getJSONObject("schema");
            }
        }
        return null;
    }

    private JSONObject schemaFromBodyObject(JSONObject body) {
        if (body == null) {
            return null;
        }
        if (body.getJSONObject("schema") != null) {
            return body.getJSONObject("schema");
        }
        JSONObject content = body.getJSONObject("content");
        if (content != null && !content.isEmpty()) {
            JSONObject media = content.getJSONObject("application/json");
            if (media == null) {
                for (String key : content.keySet()) {
                    media = content.getJSONObject(key);
                    if (media != null) {
                        break;
                    }
                }
            }
            if (media != null) {
                return media.getJSONObject("schema");
            }
        }
        return null;
    }

    private JSONObject responseSchema(JSONObject response) {
        if (response == null) {
            return null;
        }
        if (response.getJSONObject("schema") != null) {
            return response.getJSONObject("schema");
        }
        JSONObject content = response.getJSONObject("content");
        if (content != null && !content.isEmpty()) {
            JSONObject media = content.getJSONObject("application/json");
            if (media == null) {
                for (String key : content.keySet()) {
                    media = content.getJSONObject(key);
                    if (media != null) {
                        break;
                    }
                }
            }
            if (media != null) {
                return media.getJSONObject("schema");
            }
        }
        return null;
    }

    private List<SchemaField> schemaFields(JSONObject schema, JSONObject schemas) {
        List<SchemaField> fields = new ArrayList<>();
        appendSchemaFields(fields, "", schema, schemas, Collections.emptySet(), 0);
        return fields;
    }

    private void appendSchemaFields(List<SchemaField> fields, String prefix, JSONObject schema,
                                    JSONObject schemas, Set<String> required, int depth) {
        if (schema == null || depth > 3) {
            return;
        }
        JSONObject resolved = resolveSchema(schema, schemas);
        if (resolved == null) {
            return;
        }
        JSONObject properties = resolved.getJSONObject("properties");
        if (properties == null && "array".equals(resolved.getString("type"))) {
            JSONObject items = resolveSchema(resolved.getJSONObject("items"), schemas);
            if (items != null) {
                properties = items.getJSONObject("properties");
                required = requiredNames(items);
                prefix = StrUtil.isBlank(prefix) ? "[]" : prefix + "[]";
            }
        }
        if (properties == null || properties.isEmpty()) {
            if (StrUtil.isNotBlank(prefix)) {
                fields.add(new SchemaField(prefix, schemaType(resolved, schemas), required.contains(prefix), resolved.getString("description")));
            }
            return;
        }
        Set<String> requiredNames = requiredNames(resolved);
        for (String name : properties.keySet()) {
            JSONObject property = properties.getJSONObject(name);
            if (property == null) {
                continue;
            }
            JSONObject propertyResolved = resolveSchema(property, schemas);
            String fieldName = StrUtil.isBlank(prefix) ? name : prefix + "." + name;
            fields.add(new SchemaField(fieldName, schemaType(property, schemas), requiredNames.contains(name),
                    propertyResolved != null ? propertyResolved.getString("description") : property.getString("description")));
            JSONObject nested = propertyResolved != null ? propertyResolved : property;
            if (nested.getJSONObject("properties") != null
                    || nested.getString("$ref") != null
                    || ("array".equals(nested.getString("type")) && nested.getJSONObject("items") != null)) {
                appendSchemaFields(fields, fieldName, nested, schemas, requiredNames(nested), depth + 1);
            }
        }
    }

    private JSONObject resolveSchema(JSONObject schema, JSONObject schemas) {
        if (schema == null) {
            return null;
        }
        String ref = schema.getString("$ref");
        if (StrUtil.isNotBlank(ref) && schemas != null) {
            String name = ref.substring(ref.lastIndexOf('/') + 1);
            JSONObject resolved = schemas.getJSONObject(name);
            if (resolved != null) {
                return resolved;
            }
        }
        JSONArray allOf = schema.getJSONArray("allOf");
        if (allOf != null && !allOf.isEmpty() && allOf.get(0) instanceof JSONObject) {
            JSONObject merged = new JSONObject(true);
            for (Object item : allOf) {
                if (!(item instanceof JSONObject)) {
                    continue;
                }
                JSONObject part = resolveSchema((JSONObject) item, schemas);
                if (part != null) {
                    merged.putAll(part);
                }
            }
            return merged.isEmpty() ? schema : merged;
        }
        return schema;
    }

    private Set<String> requiredNames(JSONObject schema) {
        JSONArray required = schema != null ? schema.getJSONArray("required") : null;
        Set<String> names = new HashSet<>();
        if (required != null) {
            for (Object item : required) {
                names.add(String.valueOf(item));
            }
        }
        return names;
    }

    private String schemaType(JSONObject schema, JSONObject schemas) {
        if (schema == null) {
            return "";
        }
        if (StrUtil.isNotBlank(schema.getString("$ref"))) {
            return schema.getString("$ref").substring(schema.getString("$ref").lastIndexOf('/') + 1);
        }
        String type = schema.getString("type");
        if ("array".equals(type)) {
            return "array<" + schemaType(schema.getJSONObject("items"), schemas) + ">";
        }
        if (StrUtil.isBlank(type) && schema.getJSONObject("properties") != null) {
            type = "object";
        }
        String format = schema.getString("format");
        return StrUtil.isNotBlank(format) ? type + "(" + format + ")" : StrUtil.blankToDefault(type, "object");
    }

    private Object sampleFromSchema(JSONObject schema, JSONObject schemas, int depth) {
        if (schema == null || depth > 3) {
            return new LinkedHashMap<>();
        }
        JSONObject resolved = resolveSchema(schema, schemas);
        if (resolved == null) {
            return new LinkedHashMap<>();
        }
        Object example = resolved.get("example");
        if (example == null) {
            example = resolved.get("default");
        }
        if (example != null) {
            return example;
        }
        String type = resolved.getString("type");
        if ("array".equals(type)) {
            List<Object> list = new ArrayList<>();
            list.add(sampleFromSchema(resolved.getJSONObject("items"), schemas, depth + 1));
            return list;
        }
        JSONObject properties = resolved.getJSONObject("properties");
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String name : properties.keySet()) {
                map.put(name, sampleFromSchema(properties.getJSONObject(name), schemas, depth + 1));
            }
            return map;
        }
        if ("integer".equals(type) || "number".equals(type)) {
            return 0;
        }
        if ("boolean".equals(type)) {
            return false;
        }
        return "string";
    }

    private String parameterType(JSONObject param) {
        if (param == null) {
            return "";
        }
        if (param.getJSONObject("schema") != null) {
            return schemaType(param.getJSONObject("schema"), null);
        }
        String type = param.getString("type");
        if ("array".equals(type) && param.getJSONObject("items") != null) {
            return "array<" + param.getJSONObject("items").getString("type") + ">";
        }
        return StrUtil.blankToDefault(type, "string");
    }

    private String exampleValue(JSONObject param) {
        if (param == null) {
            return "";
        }
        Object example = param.get("example");
        if (example == null) {
            example = param.get("default");
        }
        if (example != null) {
            return String.valueOf(example);
        }
        String type = parameterType(param);
        if (type.startsWith("integer") || type.startsWith("number")) {
            return "0";
        }
        if (type.startsWith("boolean")) {
            return "false";
        }
        return "";
    }

    private String firstTag(OpenApiOperation op) {
        JSONArray tags = parseJsonArray(op.getTags());
        if (!tags.isEmpty()) {
            return String.valueOf(tags.get(0));
        }
        return StrUtil.blankToDefault(op.getServiceId(), "默认分组");
    }

    private JSONObject parseJsonObject(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            Object parsed = JSON.parse(raw);
            return parsed instanceof JSONObject ? (JSONObject) parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray parseJsonArray(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new JSONArray();
        }
        try {
            Object parsed = JSON.parse(raw);
            return parsed instanceof JSONArray ? (JSONArray) parsed : new JSONArray();
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private String prettyJson(Object value) {
        try {
            return JSON.toJSONString(value, true);
        } catch (Exception e) {
            return text(value);
        }
    }

    private String nowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private String exportFilename(String ext) {
        return "jbm-openapi-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "." + ext;
    }

    private String htmlRow(String key, String value) {
        return "<tr><th>" + escapeHtml(key) + "</th><td>" + escapeHtml(value) + "</td></tr>";
    }

    private String tableCell(String value) {
        return StrUtil.blankToDefault(value, "").replace("|", "\\|").replace("\r", " ").replace("\n", "<br>");
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeHtml(String value) {
        String text = StrUtil.blankToDefault(value, "");
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static class SchemaField {
        private final String name;
        private final String type;
        private final boolean required;
        private final String description;

        private SchemaField(String name, String type, boolean required, String description) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.description = description;
        }
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
        JSONObject schemas = new JSONObject(true);
        for (OpenApiOperation op : operations) {
            JSONObject opSchemas = parseJsonObject(op.getSchemasJson());
            if (opSchemas != null) {
                schemas.putAll(opSchemas);
            }
        }
        if (!schemas.isEmpty()) {
            Map<String, Object> components = new LinkedHashMap<>();
            components.put("schemas", schemas);
            spec.put("components", components);
        }
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
