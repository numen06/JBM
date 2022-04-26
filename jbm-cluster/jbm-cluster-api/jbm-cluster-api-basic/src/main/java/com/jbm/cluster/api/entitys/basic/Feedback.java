package com.jbm.cluster.api.entitys.basic;

import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel(value = "反馈管理")
public class Feedback extends MasterDataIdEntity {

    @ApiModelProperty(value = "用户ID")
    private Long userId;
    @ApiModelProperty(value = "手机号")
    private String mobilePhone;
    @ApiModelProperty(value = "邮箱")
    private String email;
    @ApiModelProperty(value = "反馈类型名称")
    private String feedbackTypeName;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "附件地址")
    private String attachmentUrl;


}
