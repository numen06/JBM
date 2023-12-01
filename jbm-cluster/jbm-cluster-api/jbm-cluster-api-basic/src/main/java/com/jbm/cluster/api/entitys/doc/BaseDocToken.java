package com.jbm.cluster.api.entitys.doc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("文档分组管理")
@TableName
public class BaseDocToken extends MasterDataEntity {

    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    @ApiModelProperty(value = "分享key")
    private String tokenKey;

    @ApiModelProperty(value = "过期时间")
    private Date expirationTime;

    @ApiModelProperty(value = "有效时间")
    private Long effectiveTime;

    @ApiModelProperty(value = "有效时间类型")
    private Integer effectiveTimeType;

//    @ApiModelProperty(value = "分享key")
//    private String tokenKey;

    @ApiModelProperty(value = "绑定的组ID")
    private String docGroupId;

    @ApiModelProperty(value = "绑定的文件ID")
    private String docId;
}
