package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName
@EqualsAndHashCode(callSuper = true)
public class MultiPlatformEntity extends MasterDataEntity {

    @ApiModelProperty("应用ID")
    private Long appId;
}
