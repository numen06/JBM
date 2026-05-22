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

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-23 16:08:36
 */
@Service
public class CustomFormsServiceImpl extends MasterDataServiceImpl<CustomForms> implements CustomFormsService {

    /** 兼容未执行 V6/V10 迁移、缺少 MasterData 通用列的旧库 */
    private static final String[] LEGACY_FORM_COLUMNS = {
            "id", "name", "menu_ids", "form_or_table", "data_source", "create_time", "update_time"
    };

    private static final String[] LEGACY_ITEM_COLUMNS = {
            "id", "form_id", "field_name", "label_name", "field_type", "component_type",
            "format", "decimal_type", "decimal_value", "choice_type", "choice_value", "date_type",
            "is_required", "is_show", "is_filter", "field_belong", "value_key", "label_key",
            "children_key", "create_time", "update_time"
    };
    @Autowired
    private CustomFormsItemService customFormsItemService;

    @Autowired(required = false)
    private ExtendFormDefinitionService extendFormDefinitionService;

    @Override
    public CustomForms saveData(CustomFormsForm form) {
        CustomForms customForms = super.saveEntity(form);
        if (CollUtil.isNotEmpty(form.getCustomFormsItemList())) {
            form.getCustomFormsItemList().forEach(item -> item.setFormId(customForms.getId()));
            customFormsItemService.saveEntitys(form.getCustomFormsItemList());
        }
        if (extendFormDefinitionService != null
                && !Boolean.FALSE.equals(form.getAutoPublishExtendField())
                && StrUtil.isNotBlank(customForms.getCode())) {
            extendFormDefinitionService.publishFromCustomForms(
                    customForms.getId(),
                    customForms.getCode(),
                    customForms.getName(),
                    CustomFormsItemToFieldDefinitionConverter.convert(form.getCustomFormsItemList()));
        }
        return customForms;
    }

    @Override
    public CustomFormsResult getDetail(CustomFormsForm form) {
        if (form == null || form.getId() == null) {
            throw new ServiceException("表单ID不能为空");
        }
        CustomForms customForms = getOne(new QueryWrapper<CustomForms>()
                .select(LEGACY_FORM_COLUMNS)
                .eq("id", form.getId()));
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