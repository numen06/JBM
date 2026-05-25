package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.cluster.common.mysql.mapper.ExtendFormDefinitionMapper;
import com.jbm.cluster.common.mysql.service.ExtendFormDefinitionService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionWriter;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
public class ExtendFormDefinitionServiceImpl implements ExtendFormDefinitionService {

    @Autowired
    private ExtendFormDefinitionMapper extendFormDefinitionMapper;

    @Autowired(required = false)
    private FieldDefinitionWriter fieldDefinitionWriter;

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Autowired
    private ExtendFieldProperties extendFieldProperties;

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Override
    public DataPaging<ExtendFormDefinition> pageByTenant(String keyword, PageForm pageForm) {
        Long tenantId = requireTenantId();
        PageForm pf = normalizePageForm(pageForm);
        QueryWrapper<ExtendFormDefinition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId);
        applyKeyword(queryWrapper, keyword);
        queryWrapper.orderByDesc("update_time").orderByDesc("id");
        Page<ExtendFormDefinition> mpPage = new Page<>(pf.getCurrPage(), pf.getPageSize());
        IPage<ExtendFormDefinition> result = extendFormDefinitionMapper.selectPage(mpPage, queryWrapper);
        return new DataPaging<>(result.getRecords(), result.getTotal(), result.getPages(), pf);
    }

    @Override
    public ExtendFormDefinition saveAndPublish(String formCode, SaveExtendFormRequest request) {
        Long tenantId = requireTenantId();
        ExtendFormDefinition row = extendFormDefinitionMapper.selectOne(new QueryWrapper<ExtendFormDefinition>()
                .eq("tenant_id", tenantId)
                .eq("form_code", formCode));
        if (row == null) {
            row = new ExtendFormDefinition();
            row.setTenantId(tenantId);
            row.setFormCode(formCode);
            row.setVersion(1);
        } else {
            row.setVersion(row.getVersion() == null ? 2 : row.getVersion() + 1);
        }
        if (request != null) {
            row.setFormName(request.getFormName());
            row.setFields(request.getFields());
            if (request.getCustomFormId() != null) {
                row.setCustomFormId(request.getCustomFormId());
            }
        }
        row.setUpdateTime(new Date());
        if (row.getId() == null) {
            extendFormDefinitionMapper.insert(row);
        } else {
            extendFormDefinitionMapper.updateById(row);
        }
        if (request == null || !Boolean.FALSE.equals(request.getAutoPublish())) {
            publishToRedis(formCode);
        }
        return row;
    }

    @Override
    public void publishToRedis(String formCode) {
        if (fieldDefinitionWriter == null) {
            throw new IllegalStateException("未配置 Redis，无法发布扩展字段定义");
        }
        ExtendFormDefinition row = loadRow(formCode);
        if (row.getFields() == null) {
            throw new IllegalArgumentException("表单定义字段为空: formCode=" + formCode);
        }
        fieldDefinitionWriter.saveFieldDefinitions(formCode, row.getFields());
    }

    @Override
    public ExtendFormDefinition getByFormCode(String formCode) {
        return loadRow(formCode);
    }

    @Override
    public void publishFromCustomForms(Long customFormId, String formCode, String formName, List<FieldDefinition> fields) {
        SaveExtendFormRequest req = new SaveExtendFormRequest();
        req.setFormName(formName);
        req.setFields(fields);
        req.setCustomFormId(customFormId);
        req.setAutoPublish(true);
        saveAndPublish(formCode, req);
    }

    @Override
    public List<FieldDefinition> listFromRedis(String formCode) {
        if (fieldDefinitionService == null) {
            throw new IllegalStateException("扩展字段定义服务未就绪");
        }
        return fieldDefinitionService.getFieldDefinitions(formCode);
    }

    private ExtendFormDefinition loadRow(String formCode) {
        Long tenantId = requireTenantId();
        ExtendFormDefinition row = extendFormDefinitionMapper.selectOne(new QueryWrapper<ExtendFormDefinition>()
                .eq("tenant_id", tenantId)
                .eq("form_code", formCode));
        if (row == null) {
            throw new IllegalArgumentException("表单定义不存在: tenantId=" + tenantId + ", formCode=" + formCode);
        }
        return row;
    }

    private Long requireTenantId() {
        Long tenantId = ExtendFieldTenantResolver.resolveTenantIdAsLong(extendFieldProperties);
        if (tenantId == null) {
            throw new IllegalStateException("缺少租户上下文，请传 "
                    + extendFieldProperties.getTenant().getHeader()
                    + " 或登录 appId，或开启 use-default-when-missing");
        }
        return tenantId;
    }

    private PageForm normalizePageForm(PageForm pageForm) {
        PageForm pf = pageForm != null ? pageForm : new PageForm();
        if (pf.getCurrPage() == null || pf.getCurrPage() < 1) {
            pf.setCurrPage(1);
        }
        if (pf.getPageSize() == null || pf.getPageSize() < 1) {
            pf.setPageSize(DEFAULT_PAGE_SIZE);
        }
        return pf;
    }

    private void applyKeyword(QueryWrapper<ExtendFormDefinition> queryWrapper, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        String kw = keyword.trim();
        queryWrapper.and(w -> w.like("form_code", kw)
                .or()
                .like("form_name", kw)
                .or()
                .apply("CAST(custom_form_id AS CHAR) LIKE {0}", "%" + kw + "%"));
    }
}
