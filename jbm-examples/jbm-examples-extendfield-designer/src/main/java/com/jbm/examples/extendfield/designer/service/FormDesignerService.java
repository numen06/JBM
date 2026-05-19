package com.jbm.examples.extendfield.designer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jbm.examples.extendfield.designer.mapper.MdExtendFormDefinitionMapper;
import com.jbm.examples.extendfield.designer.mp.MdExtendFormDefinition;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

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

    public void publishToRedis(String formCode) {
        if (fieldDefinitionWriter == null) {
            throw new IllegalStateException("未配置 Redis，无法发布字段定义");
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
