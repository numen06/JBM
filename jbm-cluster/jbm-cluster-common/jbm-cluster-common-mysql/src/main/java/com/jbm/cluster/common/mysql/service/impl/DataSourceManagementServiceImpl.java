package com.jbm.cluster.common.mysql.service.impl;


import com.jbm.cluster.api.entitys.center.DataSourceManagement;
import com.jbm.cluster.api.form.center.DataSourceManagementForm;
import com.jbm.cluster.common.mysql.service.DataSourceManagementService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-24 10:58:54
 */
@Service
public class DataSourceManagementServiceImpl extends MasterDataServiceImpl<DataSourceManagement> implements DataSourceManagementService {
    @Override
    public DataSourceManagement saveData(DataSourceManagementForm form) {
        return this.saveEntity(form);
    }
}