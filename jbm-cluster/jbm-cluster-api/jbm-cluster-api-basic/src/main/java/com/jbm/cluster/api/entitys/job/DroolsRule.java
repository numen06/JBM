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
    @ApiModelProperty("规则key")
    private String ruleKey;
    @Lob
    @ApiModelProperty("规则内容")
    private String ruleContent;
    @ApiModelProperty("版本")
    private Integer version;
}
