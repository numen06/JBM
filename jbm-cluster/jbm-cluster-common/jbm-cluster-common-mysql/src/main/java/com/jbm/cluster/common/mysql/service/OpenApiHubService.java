package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.model.api.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface OpenApiHubService {

    List<OpenApiSource> listSources();

    String getRawSpec(String serviceId);

    OpenApiOperation getOperationDetail(Long operationId);

    List<OpenApiSyncResult> sync(OpenApiSyncRequest request);

    void export(OpenApiExportRequest request, HttpServletResponse response);

    String renderHtml(OpenApiExportRequest request);

    OpenApiTestResult test(OpenApiTestRequest request, String authorization);

    OpenApiOperation saveUseCase(Long operationId, OpenApiUseCaseSaveRequest request, Long userId);
}
