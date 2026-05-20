package com.jbm.cluster.common.mysql.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.cluster.common.mysql.mapper.ExtendFormDefinitionMapper;
import com.jbm.cluster.common.mysql.service.ExtendFormDefinitionService;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionWriter;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
}
