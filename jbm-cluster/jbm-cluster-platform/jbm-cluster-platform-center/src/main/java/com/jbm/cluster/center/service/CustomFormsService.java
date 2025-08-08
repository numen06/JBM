package com.jbm.cluster.center.service;


import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.api.result.CustomFormsResult;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-23 16:08:36
 */
public interface CustomFormsService extends IMasterDataService<CustomForms> {

    /**
     * 保存自定义表单
     *
     * @param form
     * @return
     */
    CustomForms saveData(CustomFormsForm form);

    /**
     * 查询自定义表单详情
     *
     * @param form
     * @return
     */
    CustomFormsResult getDetail(CustomFormsForm form);

//    /**
//     * 分页查询自定义表单
//     *
//     * @param form
//     * @param pageForm
//     * @return
//     */
//    DataPaging<CustomFormsResult> selectPageList(CustomFormsForm form, PageForm pageForm);

}
