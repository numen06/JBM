package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.MappedSuperclass;

@Data
@TableName
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public class MultiPlatformEntity extends MasterDataIdEntity {

    @ApiModelProperty("应用ID")
    private Long applicationId;
}
