package com.jbm.cluster.api.form.center;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("保存扩展字段表单定义")
public class SaveExtendFormRequest {

    @ApiModelProperty("表单名称")
    private String formName;

    @ApiModelProperty("字段定义列表")
    private List<FieldDefinition> fields;

    @ApiModelProperty("关联 custom_forms.id")
    private Long customFormId;

    @ApiModelProperty("保存后是否发布到 Redis，默认 true")
    private Boolean autoPublish = true;
}
