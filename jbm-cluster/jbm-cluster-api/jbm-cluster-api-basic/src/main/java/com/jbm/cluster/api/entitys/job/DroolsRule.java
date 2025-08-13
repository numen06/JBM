package com.jbm.cluster.api.entitys.job;

import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author scolin
 * @description drools规则
 * @date 2025/8/4 16:40
 */
@Data
@Table
@Entity
@ApiModel("drools规则")
public class DroolsRule extends MasterDataIdEntity {
    @ApiModelProperty("规则编号")
    private String ruleCode;
    @ApiModelProperty("规则名称")
    private String ruleName;
    @ApiModelProperty("组")
    private String ruleGroup;
    @ApiModelProperty("规则描述")
    private String ruleDesc;
    @ApiModelProperty("规则状态 默认为0未启用")
    private Boolean ruleStatus = false;
    @Lob
    @ApiModelProperty("规则内容")
    private String ruleContent;
    @ApiModelProperty("版本号")
    private String version;
}
