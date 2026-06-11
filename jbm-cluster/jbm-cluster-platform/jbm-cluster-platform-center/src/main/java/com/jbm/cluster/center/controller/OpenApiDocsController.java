package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.form.OpenApiOperationForm;
import com.jbm.cluster.api.model.api.*;
import com.jbm.cluster.common.mysql.service.OpenApiHubService;
import com.jbm.cluster.common.mysql.service.OpenApiOperationService;
import com.jbm.cluster.common.mysql.service.PublishedApiDocService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;

@Api(tags = "OpenAPI 文档中心")
@RestController
@RequestMapping("/api-docs")
public class OpenApiDocsController extends BaseController {

    @Autowired
    private OpenApiHubService openApiHubService;
    @Autowired
    private OpenApiOperationService openApiOperationService;
    @Autowired
    private PublishedApiDocService publishedApiDocService;

    @ApiOperation("文档源列表")
    @GetMapping("/sources")
    public ResultBody<List<OpenApiSource>> listSources() {
        return ResultBody.callback(openApiHubService::listSources);
    }

    @ApiOperation("原始 spec")
    @GetMapping(value = "/spec/{serviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultBody<String> getSpec(@PathVariable String serviceId) {
        return ResultBody.callback(() -> openApiHubService.getRawSpec(serviceId));
    }

    @ApiOperation("接口索引")
    @GetMapping("/operations")
    public ResultBody<DataPaging<OpenApiOperationView>> listOperations(@ModelAttribute OpenApiOperationForm form) {
        final OpenApiOperationForm query = form != null ? form : new OpenApiOperationForm();
        return ResultBody.callback(() -> openApiOperationService.findOperationViews(query));
    }

    @ApiOperation("接口详情")
    @GetMapping("/operations/{operationId}")
    public ResultBody<OpenApiOperation> getOperation(@PathVariable Long operationId) {
        return ResultBody.callback(() -> openApiHubService.getOperationDetail(operationId));
    }

    @ApiOperation("保存接口测试用例")
    @PostMapping("/operations/{operationId}/use-cases")
    public ResultBody<OpenApiOperation> saveUseCase(
            @PathVariable Long operationId,
            @RequestBody OpenApiUseCaseSaveRequest request) {
        return ResultBody.callback(() -> openApiHubService.saveUseCase(operationId, request, LoginHelper.getUserId()));
    }

    @ApiOperation("同步 OpenAPI 文档")
    @PostMapping("/sync")
    public ResultBody<List<OpenApiSyncResult>> sync(@RequestBody(required = false) OpenApiSyncRequest request) {
        return ResultBody.callback(() -> openApiHubService.sync(request));
    }

    @ApiOperation("安全测试代理")
    @PostMapping("/test")
    public ResultBody<OpenApiTestResult> test(
            @RequestBody OpenApiTestRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest servletRequest) {
        request.setGatewayBaseUrl(sameOriginGatewayBaseUrl(request.getGatewayBaseUrl(), servletRequest));
        return ResultBody.callback(() -> openApiHubService.test(request, authorization));
    }

    private String sameOriginGatewayBaseUrl(String gatewayBaseUrl, HttpServletRequest request) {
        if (gatewayBaseUrl == null || gatewayBaseUrl.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(gatewayBaseUrl.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            if (uri.getHost() == null || uri.getUserInfo() != null) {
                return null;
            }
            String uriHost = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            if (!sameHost(uriHost, forwardedHost(request))
                    && !sameHost(uriHost, uriHost(request.getHeader("Origin")))
                    && !sameHost(uriHost, uriHost(request.getHeader("Referer")))) {
                return null;
            }
            return trimTrailingSlash(uri.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String forwardedHost(HttpServletRequest request) {
        String host = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (host == null || host.isEmpty()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isEmpty()) {
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                    || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
            host = request.getServerName() + (defaultPort ? "" : ":" + port);
        }
        return host;
    }

    private boolean sameHost(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String uriHost(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(firstHeaderValue(value));
            if (uri.getHost() == null) {
                return null;
            }
            return uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        int comma = value.indexOf(',');
        return (comma >= 0 ? value.substring(0, comma) : value).trim();
    }

    private String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    @ApiOperation("导出文档")
    @PostMapping("/export")
    public void export(@RequestBody OpenApiExportRequest request, HttpServletResponse response) {
        openApiHubService.export(request, response);
    }

    @ApiOperation("发布公开文档")
    @PostMapping("/publish")
    public ResultBody<?> publish(@RequestBody OpenApiPublishRequest request) {
        return ResultBody.callback(() -> publishedApiDocService.publish(request, LoginHelper.getUserId()));
    }
}
