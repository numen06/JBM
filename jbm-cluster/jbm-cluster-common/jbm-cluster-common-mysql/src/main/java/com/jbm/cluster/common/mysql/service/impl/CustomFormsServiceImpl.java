package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.api.result.CustomFormsResult;
import com.jbm.cluster.common.mysql.extendfield.CustomFormsItemToFieldDefinitionConverter;
import com.jbm.cluster.common.mysql.service.CustomFormsItemService;
import com.jbm.cluster.common.mysql.service.CustomFormsService;
import com.jbm.cluster.common.mysql.service.ExtendFormDefinitionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-23 16:08:36
 */
@Service
public class CustomFormsServiceImpl extends MasterDataServiceImpl<CustomForms> implements CustomFormsService {

    /** 兼容未执行 V20 前、部分设计态列缺失的旧库；V20 会补齐这些列。 */
    private static final String[] LEGACY_FORM_COLUMNS = {
            "id", "code", "name", "menu_ids", "form_or_table", "data_source", "detail",
            "app_id", "parent_id", "level", "leaf_path", "extend_data", "create_time", "update_time"
    };

    private static final String[] LEGACY_ITEM_COLUMNS = {
            "id", "form_id", "field_name", "label_name", "field_type", "component_type",
            "format", "decimal_type", "decimal_value", "choice_type", "choice_value", "date_type",
            "is_required", "is_show", "is_filter", "field_belong", "value_key", "label_key",
            "children_key", "code", "app_id", "parent_id", "level", "leaf_path",
            "extend_data", "create_time", "update_time"
    };
    @Autowired
    private CustomFormsItemService customFormsItemService;

    @Autowired(required = false)
    private ExtendFormDefinitionService extendFormDefinitionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomForms saveData(CustomFormsForm form) {
        if (form == null) {
            throw new ServiceException("表单参数不能为空");
        }
        boolean autoPublish = !Boolean.FALSE.equals(form.getAutoPublishExtendField());
        if (autoPublish && StrUtil.isBlank(form.getCode())) {
            throw new ServiceException("自动发布扩展字段时表单编码 code/formCode 不能为空");
        }
        CustomForms existing = null;
        if (form.getId() == null && StrUtil.isNotBlank(form.getCode())) {
            existing = getOne(new QueryWrapper<CustomForms>()
                    .select("id", "detail")
                    .eq("code", form.getCode())
                    .last("LIMIT 1"));
            if (existing != null) {
                form.setId(existing.getId());
            }
        }
        if (form.getId() != null && form.getDetail() == null) {
            if (existing == null) {
                existing = getOne(new QueryWrapper<CustomForms>()
                        .select("id", "detail")
                        .eq("id", form.getId())
                        .last("LIMIT 1"));
            }
            if (existing != null) {
                form.setDetail(existing.getDetail());
            }
        }
        CustomForms customForms = super.saveEntity(form);
        if (form.getCustomFormsItemList() != null) {
            customFormsItemService.remove(new QueryWrapper<CustomFormsItem>()
                    .eq("form_id", customForms.getId()));
            form.getCustomFormsItemList().forEach(item -> {
                item.setId(null);
                item.setFormId(customForms.getId());
            });
        }
        if (CollUtil.isNotEmpty(form.getCustomFormsItemList())) {
            customFormsItemService.saveEntitys(form.getCustomFormsItemList());
        }
        if (extendFormDefinitionService != null
                && autoPublish) {
            List<CustomFormsItem> publishItems = form.getCustomFormsItemList();
            if (publishItems == null) {
                publishItems = customFormsItemService.list(new QueryWrapper<CustomFormsItem>()
                        .select(LEGACY_ITEM_COLUMNS)
                        .eq("form_id", customForms.getId()));
            }
            extendFormDefinitionService.publishFromCustomForms(
                    customForms.getId(),
                    customForms.getCode(),
                    customForms.getName(),
                    CustomFormsItemToFieldDefinitionConverter.convert(publishItems));
        }
        return customForms;
    }

    @Override
    public CustomFormsResult getDetail(CustomFormsForm form) {
        if (form == null || (form.getId() == null && StrUtil.isBlank(form.getCode()))) {
            throw new ServiceException("表单ID或编码不能为空");
        }
        QueryWrapper<CustomForms> queryWrapper = new QueryWrapper<CustomForms>()
                .select(LEGACY_FORM_COLUMNS);
        if (form.getId() != null) {
            queryWrapper.eq("id", form.getId());
        } else {
            queryWrapper.eq("code", form.getCode());
        }
        CustomForms customForms = getOne(queryWrapper.last("LIMIT 1"));
        if (customForms == null) {
            throw new ServiceException("自定义表单不存在");
        }
        CustomFormsResult customFormsResult = BeanUtil.copyProperties(customForms, CustomFormsResult.class);
        customFormsResult.setCustomFormsItemList(customFormsItemService.list(
                new QueryWrapper<CustomFormsItem>()
                        .select(LEGACY_ITEM_COLUMNS)
                        .eq("form_id", customForms.getId())));
        return customFormsResult;
    }

//    @Override
//    public DataPaging<CustomFormsResult> selectPageList(CustomFormsForm form, PageForm pageForm) {
//        DataPaging<CustomForms> customFormsDataPaging = selectEntitys(form, pageForm);
//        return BeanUtil.copyProperties(customFormsDataPaging, DataPaging.class);
//    }
}
