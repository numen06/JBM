package com.jbm.cluster.api.form.center;


import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 16:16
 */
@Data
@ApiModel("自定义表单请求体")
public class CustomFormsForm extends CustomForms {
    @ApiModelProperty("分页封装实体")
    private PageForm pageForm;
    @ApiModelProperty("表单字段明细")
    private List<CustomFormsItem> customFormsItemList;
}
