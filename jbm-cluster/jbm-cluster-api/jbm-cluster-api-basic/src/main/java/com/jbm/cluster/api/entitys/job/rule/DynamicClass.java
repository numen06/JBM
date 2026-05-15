package com.jbm.cluster.api.entitys.job.rule;

import com.baomidou.mybatisplus.annotation.TableField;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author scolin
 * @description 动态模版类
 * @date 2025/8/5 16:52
 */
@Data
@Table
@Entity
@ApiModel("动态模版类")
public class DynamicClass extends MasterDataEntity {

    private static final long serialVersionUID = 1L;
    @NotNull
    @ApiModelProperty("类名")
    private String className;
    @ApiModelProperty("包名")
    private String packageName;
    @ApiModelProperty("描述")
    private String description;
}
