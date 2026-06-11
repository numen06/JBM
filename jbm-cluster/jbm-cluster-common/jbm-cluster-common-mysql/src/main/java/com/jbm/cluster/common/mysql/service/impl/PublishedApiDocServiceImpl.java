package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.OpenApiDocument;
import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.entitys.basic.PublishedApiDoc;
import com.jbm.cluster.api.form.OpenApiOperationForm;
import com.jbm.cluster.api.model.api.OpenApiExportRequest;
import com.jbm.cluster.api.model.api.OpenApiPublishRequest;
import com.jbm.cluster.common.mysql.mapper.PublishedApiDocMapper;
import com.jbm.cluster.common.mysql.service.OpenApiHubService;
import com.jbm.cluster.common.mysql.service.OpenApiOperationService;
import com.jbm.cluster.common.mysql.service.PublishedApiDocService;
import com.jbm.cluster.common.mysql.service.openapi.OpenApiHubSupport;
import com.jbm.cluster.common.mysql.service.openapi.OpenApiSpecSanitizer;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PublishedApiDocServiceImpl extends MasterDataServiceImpl<PublishedApiDoc> implements PublishedApiDocService {

    @Autowired
    private PublishedApiDocMapper publishedApiDocMapper;
    @Autowired
    private OpenApiOperationService openApiOperationService;
    @Autowired
    private OpenApiSpecSanitizer openApiSpecSanitizer;
    @Autowired
    private OpenApiHubService openApiHubService;

    @Override
    public List<PublishedApiDoc> listActive() {
        QueryWrapper<PublishedApiDoc> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(PublishedApiDoc::getStatus, 1);
        wrapper.orderByDesc("published_at");
        return publishedApiDocMapper.selectList(wrapper);
    }

    @Override
    public PublishedApiDoc getByDocKey(String docKey) {
        QueryWrapper<PublishedApiDoc> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(PublishedApiDoc::getDocKey, docKey);
        return publishedApiDocMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublishedApiDoc publish(OpenApiPublishRequest request, Long publisherUserId) {
        if (request == null) {
            throw new ServiceException("请选择要发布的接口");
        }
        List<OpenApiOperation> selected = resolveOperations(request);
        boolean html = StrUtil.equalsIgnoreCase(request.getFormat(), "HTML");
        List<OpenApiOperation> publishable = html ? selected : openApiSpecSanitizer.filterPublishable(selected);
        if (publishable.isEmpty() && html) {
            throw new ServiceException("没有可预览的接口");
        }
        if (publishable.isEmpty()) {
            throw new ServiceException("所选接口均不满足发布条件（需启用、已开放且已关联 API 资源）");
        }
        List<Long> publishableIds = publishable.stream()
                .map(OpenApiOperation::getOperationId)
                .collect(java.util.stream.Collectors.toList());
        String docKey = StrUtil.blankToDefault(request.getDocKey(), "default");
        String spec = html
                ? openApiHubService.renderHtml(exportRequest(publishableIds))
                : openApiSpecSanitizer.buildPublishedSpec(request.getTitle(), request.getVersion(), publishable);
        Date now = new Date();
        PublishedApiDoc doc = getByDocKey(docKey);
        if (doc == null) {
            doc = new PublishedApiDoc();
            doc.setDocKey(docKey);
            doc.setCreateTime(now);
        }
        doc.setTitle(StrUtil.blankToDefault(request.getTitle(), "JBM Open API"));
        doc.setVersion(StrUtil.blankToDefault(request.getVersion(), "1.0.0"));
        doc.setContentType(html ? "text/html" : "application/json");
        doc.setPublishedSpec(spec);
        doc.setPublishedSummary(request.getPublishedSummary());
        doc.setSourceHash(OpenApiHubSupport.sha256(spec));
        doc.setPublisherUserId(publisherUserId);
        doc.setPublishedAt(now);
        doc.setStatus(1);
        doc.setUpdateTime(now);
        if (doc.getPublishedId() == null) {
            publishedApiDocMapper.insert(doc);
        } else {
            publishedApiDocMapper.updateById(doc);
        }
        return doc;
    }

    private List<OpenApiOperation> resolveOperations(OpenApiPublishRequest request) {
        List<OpenApiOperation> selected = new ArrayList<>();
        if (CollUtil.isNotEmpty(request.getOperationIds())) {
            for (Long operationId : request.getOperationIds()) {
                OpenApiOperation op = openApiOperationService.getById(operationId);
                if (op != null) {
                    selected.add(op);
                }
            }
            return selected;
        }
        OpenApiOperationForm form = new OpenApiOperationForm();
        if (CollUtil.isNotEmpty(request.getServiceIds())) {
            form.setServiceId(request.getServiceIds().get(0));
        }
        if (request.getFilters() != null) {
            Object serviceId = request.getFilters().get("serviceId");
            if (serviceId != null) {
                form.setServiceId(String.valueOf(serviceId));
            }
            Object keyword = request.getFilters().get("keyword");
            if (keyword != null) {
                form.setKeyword(String.valueOf(keyword));
            }
            Object method = request.getFilters().get("method");
            if (method != null) {
                form.setMethod(String.valueOf(method));
            }
            Object isOpen = request.getFilters().get("isOpen");
            if (isOpen != null) {
                form.setIsOpen(Integer.valueOf(String.valueOf(isOpen)));
            }
            Object isAuth = request.getFilters().get("isAuth");
            if (isAuth != null) {
                form.setIsAuth(Integer.valueOf(String.valueOf(isAuth)));
            }
            Object syncState = request.getFilters().get("syncState");
            if (syncState != null) {
                form.setSyncState(String.valueOf(syncState));
            }
        }
        com.jbm.framework.usage.paging.PageForm pageForm = new com.jbm.framework.usage.paging.PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(10000);
        form.setPageForm(pageForm);
        return openApiOperationService.findOperationViews(form).getContents().stream()
                .map(view -> openApiOperationService.getById(view.getOperationId()))
                .filter(op -> op != null)
                .collect(java.util.stream.Collectors.toList());
    }

    private OpenApiExportRequest exportRequest(List<Long> operationIds) {
        OpenApiExportRequest request = new OpenApiExportRequest();
        request.setFormat("HTML");
        request.setSelectionMode("CHECKED");
        request.setOperationIds(operationIds);
        request.setIncludeSchemas(true);
        request.setIncludeExamples(true);
        request.setIncludeGovernance(true);
        return request;
    }
}
