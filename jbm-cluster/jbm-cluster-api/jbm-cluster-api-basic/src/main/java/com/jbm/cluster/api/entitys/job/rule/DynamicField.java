package com.jbm.cluster.api.entitys.job.rule;

import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author scolin
 * @description 动态模版类字段
 * @date 2025/8/5 17:00
 */
@Data
@Table
@Entity
@ApiModel("动态模版类字段")
public class DynamicField extends MasterDataIdEntity {
    private static final long serialVersionUID = 1L;
    @NotNull
    @ApiModelProperty("类id")
    private Long classId;
    @NotNull
    @ApiModelProperty("字段名")
    private String fieldName;
    @NotNull
    @ApiModelProperty("字段类型")
    private String fieldType;
    @ApiModelProperty("字段描述")
    private String fieldLabel;

}
