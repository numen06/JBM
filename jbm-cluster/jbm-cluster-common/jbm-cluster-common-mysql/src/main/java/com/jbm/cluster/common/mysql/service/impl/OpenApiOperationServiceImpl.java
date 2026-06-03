package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.form.OpenApiOperationForm;
import com.jbm.cluster.api.model.api.OpenApiOperationView;
import com.jbm.cluster.common.mysql.mapper.OpenApiOperationMapper;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.common.mysql.service.OpenApiOperationService;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenApiOperationServiceImpl extends MasterDataServiceImpl<OpenApiOperation> implements OpenApiOperationService {

    @Autowired
    private OpenApiOperationMapper openApiOperationMapper;
    @Autowired
    private BaseApiService baseApiService;

    @Override
    public DataPaging<OpenApiOperationView> findOperationViews(OpenApiOperationForm form) {
        QueryWrapper<OpenApiOperation> wrapper = buildQueryWrapper(form);
        wrapper.orderByAsc("service_id", "path", "request_method");
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        DataPaging<OpenApiOperation> paging = selectEntitys(PageParams.from(pageForm), wrapper);
        List<OpenApiOperationView> views = new ArrayList<>();
        if (paging.getContents() != null) {
            for (OpenApiOperation op : paging.getContents()) {
                views.add(toView(op));
            }
        }
        DataPaging<OpenApiOperationView> result = new DataPaging<>();
        result.setContents(views);
        result.setTotal(paging.getTotal());
        result.setPageForm(paging.getPageForm());
        return result;
    }

    @Override
    public OpenApiOperation getByOperationKey(String operationKey) {
        QueryWrapper<OpenApiOperation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OpenApiOperation::getOperationKey, operationKey);
        return openApiOperationMapper.selectOne(wrapper);
    }

    @Override
    public List<OpenApiOperation> listByServiceId(String serviceId) {
        QueryWrapper<OpenApiOperation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OpenApiOperation::getServiceId, serviceId);
        return openApiOperationMapper.selectList(wrapper);
    }

    @Override
    public int countByServiceId(String serviceId, String syncState) {
        QueryWrapper<OpenApiOperation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OpenApiOperation::getServiceId, serviceId);
        if (StrUtil.isNotBlank(syncState)) {
            wrapper.lambda().eq(OpenApiOperation::getSyncState, syncState);
        } else {
            wrapper.lambda().ne(OpenApiOperation::getSyncState, "MISSING");
        }
        return Math.toIntExact(openApiOperationMapper.selectCount(wrapper));
    }

    @Override
    public int countLinkedByServiceId(String serviceId) {
        QueryWrapper<OpenApiOperation> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(OpenApiOperation::getServiceId, serviceId)
                .isNotNull(OpenApiOperation::getApiId)
                .ne(OpenApiOperation::getSyncState, "MISSING");
        return Math.toIntExact(openApiOperationMapper.selectCount(wrapper));
    }

    private QueryWrapper<OpenApiOperation> buildQueryWrapper(OpenApiOperationForm form) {
        QueryWrapper<OpenApiOperation> wrapper = new QueryWrapper<>();
        if (form == null) {
            wrapper.lambda().ne(OpenApiOperation::getSyncState, "MISSING");
            return wrapper;
        }
        wrapper.lambda()
                .eq(ObjectUtils.isNotEmpty(form.getServiceId()), OpenApiOperation::getServiceId, form.getServiceId())
                .eq(ObjectUtils.isNotEmpty(form.getMethod()), OpenApiOperation::getRequestMethod, form.getMethod())
                .eq(ObjectUtils.isNotEmpty(form.getIsOpen()), OpenApiOperation::getIsOpen, form.getIsOpen())
                .eq(ObjectUtils.isNotEmpty(form.getIsAuth()), OpenApiOperation::getIsAuth, form.getIsAuth())
                .eq(ObjectUtils.isNotEmpty(form.getStatus()), OpenApiOperation::getStatus, form.getStatus())
                .eq(ObjectUtils.isNotEmpty(form.getSyncState()), OpenApiOperation::getSyncState, form.getSyncState());
        if (Boolean.TRUE.equals(form.getLinked())) {
            wrapper.lambda().isNotNull(OpenApiOperation::getApiId);
        } else if (Boolean.FALSE.equals(form.getLinked())) {
            wrapper.lambda().isNull(OpenApiOperation::getApiId);
        }
        if (StrUtil.isNotBlank(form.getTag())) {
            wrapper.lambda().like(OpenApiOperation::getTags, form.getTag());
        }
        if (StrUtil.isNotBlank(form.getKeyword())) {
            String kw = form.getKeyword().trim();
            wrapper.and(w -> w.like("path", kw).or().like("summary", kw).or().like("description", kw));
        }
        if (StrUtil.isBlank(form.getSyncState())) {
            wrapper.lambda().ne(OpenApiOperation::getSyncState, "MISSING");
        }
        return wrapper;
    }

    private OpenApiOperationView toView(OpenApiOperation op) {
        OpenApiOperationView view = new OpenApiOperationView();
        view.setOperationId(op.getOperationId());
        view.setServiceId(op.getServiceId());
        view.setMethod(op.getRequestMethod());
        view.setPath(op.getPath());
        view.setSummary(op.getSummary());
        view.setApiId(op.getApiId());
        view.setIsOpen(op.getIsOpen());
        view.setIsAuth(op.getIsAuth() != null && op.getIsAuth() == 1);
        view.setStatus(op.getStatus());
        view.setLinked(op.getApiId() != null);
        view.setSyncState(op.getSyncState());
        view.setDeprecated(op.getDeprecated());
        if (StrUtil.isNotBlank(op.getTags())) {
            try {
                List<String> tags = JSON.parseArray(op.getTags(), String.class);
                if (tags != null && !tags.isEmpty()) {
                    view.setTag(tags.get(0));
                }
            } catch (Exception ignored) {
                view.setTag(op.getTags());
            }
        }
        if (op.getApiId() != null) {
            BaseApi api = baseApiService.getApi(op.getApiId());
            if (api != null) {
                view.setApiCode(api.getApiCode());
            }
        }
        return view;
    }
}
