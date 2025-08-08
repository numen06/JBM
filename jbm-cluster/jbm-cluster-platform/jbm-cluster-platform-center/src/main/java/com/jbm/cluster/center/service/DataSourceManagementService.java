package com.jbm.cluster.center.service;


import com.jbm.cluster.api.entitys.center.DataSourceManagement;
import com.jbm.cluster.api.form.center.DataSourceManagementForm;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-24 10:58:54
 */
public interface DataSourceManagementService extends IMasterDataService<DataSourceManagement> {

    /**
     * 保存数据源
     *
     * @param form
     * @return
     */
    DataSourceManagement saveData(DataSourceManagementForm form);

}
