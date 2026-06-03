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
import com.jbm.cluster.api.model.api.OpenApiPublishRequest;
import com.jbm.cluster.common.mysql.mapper.PublishedApiDocMapper;
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
        if (request == null || CollUtil.isEmpty(request.getOperationIds())) {
            throw new ServiceException("请选择要发布的接口");
        }
        List<OpenApiOperation> selected = new ArrayList<>();
        for (Long operationId : request.getOperationIds()) {
            OpenApiOperation op = openApiOperationService.getById(operationId);
            if (op != null) {
                selected.add(op);
            }
        }
        List<OpenApiOperation> publishable = openApiSpecSanitizer.filterPublishable(selected);
        if (publishable.isEmpty()) {
            throw new ServiceException("所选接口均不满足发布条件（需启用、已开放且已关联 API 资源）");
        }
        String docKey = StrUtil.blankToDefault(request.getDocKey(), "default");
        String spec = openApiSpecSanitizer.buildPublishedSpec(request.getTitle(), request.getVersion(), publishable);
        Date now = new Date();
        PublishedApiDoc doc = getByDocKey(docKey);
        if (doc == null) {
            doc = new PublishedApiDoc();
            doc.setDocKey(docKey);
            doc.setCreateTime(now);
        }
        doc.setTitle(StrUtil.blankToDefault(request.getTitle(), "JBM Open API"));
        doc.setVersion(StrUtil.blankToDefault(request.getVersion(), "1.0.0"));
        doc.setContentType("application/json");
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
}
