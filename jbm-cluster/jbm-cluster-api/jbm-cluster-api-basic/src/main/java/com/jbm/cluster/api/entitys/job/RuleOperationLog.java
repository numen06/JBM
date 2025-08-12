package com.jbm.cluster.api.entitys.job;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * @author scolin
 * @description
 * @date 2025/8/12 17:56
 */
@Data
@Table
@Entity
@ApiModel("规则操作日志")
public class RuleOperationLog extends MasterDataIdEntity {

    @ApiModelProperty("规则id")
    private Long ruleId;
    @ApiModelProperty("规则编号")
    private String ruleCode;
    @ApiModelProperty("规则名称")
    private String ruleName;
    @ApiModelProperty("组")
    private String ruleGroup;
    @ApiModelProperty("规则描述")
    private String ruleDesc;
    @ApiModelProperty("规则状态")
    private Boolean ruleStatus;
    @Lob
    @ApiModelProperty("规则内容")
    private String ruleContent;
    @ApiModelProperty("版本号")
    private String version;
    @ApiModelProperty("操作人账号")
    private String operationUser;
    @ApiModelProperty("操作人名称")
    private String operationUserName;
    @ApiModelProperty("操作时间")
    private Date operationTime;

}
