package com.jbm.cluster.center.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.api.result.CustomFormsResult;
import com.jbm.cluster.center.service.CustomFormsItemService;
import com.jbm.cluster.center.service.CustomFormsService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-23 16:08:36
 */
@Service
public class CustomFormsServiceImpl extends MasterDataServiceImpl<CustomForms> implements CustomFormsService {
    @Autowired
    private CustomFormsItemService customFormsItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomForms saveData(CustomFormsForm form) {
        //保存主表
        CustomForms customForms = this.saveEntity(form);
        //保存子表
        if(CollUtil.isNotEmpty(form.getCustomFormsItemList())){
            form.getCustomFormsItemList().forEach(item -> item.setFormId(customForms.getId()));
            customFormsItemService.saveEntitys(form.getCustomFormsItemList());
        }
        return customForms;
    }

    @Override
    public CustomFormsResult getDetail(CustomFormsForm form) {
        CustomForms customForms = this.selectEntity(form);
        CustomFormsResult customFormsResult = BeanUtil.copyProperties(customForms, CustomFormsResult.class);
        CustomFormsItem customFormsItem = new CustomFormsItem();
        customFormsItem.setFormId(customForms.getId());
        customFormsResult.setCustomFormsItemList(customFormsItemService.selectEntitys(customFormsItem));
        return customFormsResult;
    }

//    @Override
//    public DataPaging<CustomFormsResult> selectPageList(CustomFormsForm form, PageForm pageForm) {
//        DataPaging<CustomForms> customFormsDataPaging = selectEntitys(form, pageForm);
//        return BeanUtil.copyProperties(customFormsDataPaging, DataPaging.class);
//    }
}