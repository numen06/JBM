package com.jbm.cluster.api.form.doc;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@ApiModel("带Token的删除")
public class DocPathForm {

//    @ApiModelProperty(value = "分享key")
//    private String tokenKey;

    @ApiModelProperty("访问路径")
    private List<String> paths;
    @ApiModelProperty("访问路径")
    private List<String> ids;
}
