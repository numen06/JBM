package com.jbm.cluster.api.form.center;

import com.jbm.cluster.api.entitys.center.DataSourceManagement;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author scolin
 * @description
 * @date 2025/7/24 11:12
 */
@Data
@ApiModel("数据源请求体")
public class DataSourceManagementForm extends DataSourceManagement {
    @ApiModelProperty("分页封装实体")
    private PageForm pageForm;
}
