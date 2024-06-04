package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseRole;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author fanscat
 * @createTime 2024/6/4 13:56
 */
@ApiModel("系统角色表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseRoleForm extends BaseRole {
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("时间范围")
    private Date[] dateRange;
    @ApiModelProperty("开始时间")
    private Date beginTime;
    @ApiModelProperty("结束时间")
    private Date endTime;
}
