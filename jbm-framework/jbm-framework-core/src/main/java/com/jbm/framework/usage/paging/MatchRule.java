package com.jbm.framework.usage.paging;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 匹配规则
 */
@Data
@ApiModel(value = "规则实体")
public class MatchRule {
    @ApiModelProperty(value = "匹配的列")
    private String col;
    @ApiModelProperty(value = "规则:like,notLike,likeLeft,likeRightlike,eq,between,in,notIn,inSql,notinSql,exists,notExists")
    private String rule;
    @ApiModelProperty(value = "连接符")
    private String joiner;
    private List<String> values;
}
