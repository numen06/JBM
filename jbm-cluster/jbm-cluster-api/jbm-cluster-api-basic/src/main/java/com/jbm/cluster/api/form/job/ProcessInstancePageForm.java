package com.jbm.cluster.api.form.job;

import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author scolin
 * @description 流程实例分页查询表单
 * @date 2025/11/17
 */
@ApiModel("流程实例分页查询表单")
@Data
public class ProcessInstancePageForm {
    @ApiModelProperty("规则定义ID，可为空表示查询所有")
    private Long ruleDefinitionId;

    @ApiModelProperty("流程状态，可为空表示查询所有")
    private String status;

    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}
