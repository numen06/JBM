package com.jbm.cluster.api.entitys.doc;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.Date;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("文档分组管理")
@TableName
public class BaseDocToken extends MasterDataIdEntity {
    @ApiModelProperty(value = "过期时间")
    private Date expirationTime;

    @ApiModelProperty(value = "有效时间")
    private Long effectiveTime;

    @ApiModelProperty(value = "有效时间类型")
    private Integer effectiveTimeType;

    @ApiModelProperty(value = "分享key")
    private String tokenKey;
}
