package com.jbm.cluster.api.form.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

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
    private String transTaskId;
    private String aaaa;

    @ApiModelProperty("任务列表")
    @JsonProperty("Tasks")
    private List<AgvTaskItem> tasks;

    @Data
    public static class AgvTaskItem {
        @ApiModelProperty("任务类型")
        @JsonProperty("Type")
        private String type;

        @ApiModelProperty("站点编号")
        @JsonProperty("Station")
        private String station;

        @ApiModelProperty("层级")
        @JsonProperty("Layer")
        private String layer;

        private String transTaskId;
    }
}
