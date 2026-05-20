package com.jbm.micro.mysql.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jbm.micro.mysql.mapper.MdExtendFormDefinitionMapper;
import com.jbm.micro.mysql.mp.MdExtendFormDefinition;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionWriter;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 模拟「表单/配置微服务」：定义按租户入库为真源，再发布到 Redis 供业务侧读取。
 */
@Service
public class FormDesignerService extends ServiceImpl<MdExtendFormDefinitionMapper, MdExtendFormDefinition> {

    @Autowired(required = false)
    private FieldDefinitionWriter fieldDefinitionWriter;

    @Autowired
    private ExtendFieldProperties extendFieldProperties;

    @Transactional(rollbackFor = Exception.class)
    public MdExtendFormDefinition saveAndPublish(String formCode, String formName, List<FieldDefinition> fields) {
        Long tenantId = requireTenantId();
        MdExtendFormDefinition row = getOne(new QueryWrapper<MdExtendFormDefinition>()
                .eq("tenant_id", tenantId)
                .eq("form_code", formCode));
        if (row == null) {
            row = new MdExtendFormDefinition();
            row.setTenantId(tenantId);
            row.setFormCode(formCode);
            row.setVersion(1);
        } else {
            row.setVersion(row.getVersion() == null ? 2 : row.getVersion() + 1);
        }
        row.setFormName(formName);
        row.setFields(fields);
        row.setUpdateTime(new Date());
        saveOrUpdate(row);
        publishToRedis(formCode);
        return row;
    }

    public void publishToRedis(String formCode) {
        if (fieldDefinitionWriter == null) {
            throw new IllegalStateException("未配置 Redis，无法发布字段定义（需 REDIS 模式 + autoconfigure-redis）");
        }
        Long tenantId = requireTenantId();
        MdExtendFormDefinition row = getOne(new QueryWrapper<MdExtendFormDefinition>()
                .eq("tenant_id", tenantId)
                .eq("form_code", formCode));
        if (row == null || row.getFields() == null) {
            throw new IllegalArgumentException("表单定义不存在: tenantId=" + tenantId + ", formCode=" + formCode);
        }
        fieldDefinitionWriter.saveFieldDefinitions(formCode, row.getFields());
    }

    public MdExtendFormDefinition getByFormCode(String formCode) {
        Long tenantId = requireTenantId();
        return getOne(new QueryWrapper<MdExtendFormDefinition>()
                .eq("tenant_id", tenantId)
                .eq("form_code", formCode));
    }

    private Long requireTenantId() {
        Long tenantId = ExtendFieldTenantResolver.resolveTenantIdAsLong(extendFieldProperties);
        if (tenantId == null) {
            throw new IllegalStateException("缺少租户上下文，请传请求头 "
                    + extendFieldProperties.getTenant().getHeader()
                    + " 或开启 jbm.extend-field.tenant.use-default-when-missing");
        }
        return tenantId;
    }
}
