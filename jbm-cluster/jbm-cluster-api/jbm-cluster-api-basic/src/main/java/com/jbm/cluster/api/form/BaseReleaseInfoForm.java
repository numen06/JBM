package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseReleaseInfo;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("版本发布表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseReleaseInfoForm extends BaseReleaseInfo {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}