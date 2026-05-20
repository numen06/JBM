package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseUserCertification;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("用户实名认证表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseUserCertificationForm extends BaseUserCertification {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}