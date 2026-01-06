package com.jbm.cluster.api.form.job;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author scolin
 * @description
 * @date 2025/11/28 14:00
 */
@ApiModel("AGV测试表单")
@Data
public class AgvTestForm {
    @ApiModelProperty("出发地")
    private String startLocation;
    @ApiModelProperty("目的地")
    private String endLocation;
    @ApiModelProperty("出发编号")
    private String startCode;
    @ApiModelProperty("目的编号")
    private String endCode;
    private String www;
    private String aaaa;
}
