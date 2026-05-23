package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@ApiModel("API Key表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseApiKeyForm extends BaseApiKey {

    @ApiModelProperty("分页参数")
    private PageForm pageForm;

    @ApiModelProperty("授权权限ID列表")
    private List<String> authorityIds;

    @ApiModelProperty("授权过期时间")
    private Date authorityExpireTime;
}
