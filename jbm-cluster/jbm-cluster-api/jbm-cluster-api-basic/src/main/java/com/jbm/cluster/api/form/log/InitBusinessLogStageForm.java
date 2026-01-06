package com.jbm.cluster.api.form.log;

import com.jbm.cluster.api.entitys.log.BusinessLogStageItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 初始化业务日志阶段配置
 */
@Data
@ApiModel(value = "初始化业务日志阶段配置表单")
public class InitBusinessLogStageForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务日志ID", required = true)
    @NotBlank(message = "logId不能为空")
    private String logId;

    @ApiModelProperty(value = "阶段列表", required = true)
    @NotEmpty(message = "至少需要一个阶段")
    private List<BusinessLogStageItem> stages;
}


