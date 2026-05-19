package com.jbm.micro.mysql.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jbm.micro.mysql.mapper.MdExtendFormDefinitionMapper;
import com.jbm.micro.mysql.mp.MdExtendFormDefinition;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 模拟「表单/配置微服务」：定义入库为真源，再发布到 Redis 供业务侧读取。
 */
@Service
public class FormDesignerService extends ServiceImpl<MdExtendFormDefinitionMapper, MdExtendFormDefinition> {

    @Autowired(required = false)
    private FieldDefinitionWriter fieldDefinitionWriter;

    @Transactional(rollbackFor = Exception.class)
    public MdExtendFormDefinition saveAndPublish(String formCode, String formName, List<FieldDefinition> fields) {
        MdExtendFormDefinition row = getOne(new QueryWrapper<MdExtendFormDefinition>().eq("form_code", formCode));
        if (row == null) {
            row = new MdExtendFormDefinition();
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

    /**
     * 从库加载并刷新 Redis（模拟业务服务只读 Redis 前的发布动作）。
     */
    public void publishToRedis(String formCode) {
        if (fieldDefinitionWriter == null) {
            throw new IllegalStateException("未配置 Redis，无法发布字段定义（需 REDIS 模式 + autoconfigure-redis）");
        }
        MdExtendFormDefinition row = getOne(new QueryWrapper<MdExtendFormDefinition>().eq("form_code", formCode));
        if (row == null || row.getFields() == null) {
            throw new IllegalArgumentException("表单定义不存在: " + formCode);
        }
        fieldDefinitionWriter.saveFieldDefinitions(formCode, row.getFields());
    }

    public MdExtendFormDefinition getByFormCode(String formCode) {
        return getOne(new QueryWrapper<MdExtendFormDefinition>().eq("form_code", formCode));
    }
}
