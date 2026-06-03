package com.jbm.cluster.api.form;

import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("OpenAPI 接口查询")
public class OpenApiOperationForm {

    @ApiModelProperty("服务 ID")
    private String serviceId;

    @ApiModelProperty("关键字")
    private String keyword;

    @ApiModelProperty("HTTP 方法")
    private String method;

    @ApiModelProperty("是否开放 0/1")
    private Integer isOpen;

    @ApiModelProperty("是否认证 0/1")
    private Integer isAuth;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("同步状态")
    private String syncState;

    @ApiModelProperty("是否已关联 base_api")
    private Boolean linked;

    @ApiModelProperty("标签")
    private String tag;

    @ApiModelProperty("分页")
    private PageForm pageForm;
}
