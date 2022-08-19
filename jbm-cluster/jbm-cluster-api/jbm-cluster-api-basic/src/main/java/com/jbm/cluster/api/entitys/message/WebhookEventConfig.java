package com.jbm.cluster.api.entitys.message;


import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
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
@TableName
@ApiModel("Web反向推送配置")
public class WebhookEventConfig extends MultiPlatformEntity {

    @ApiModelProperty("业务事件ID")
    private String businessEventId;

    @ApiModelProperty("事件名称")
    private String eventName;

    @ApiModelProperty("是否内部")
    private Boolean internal;

    @ApiModelProperty("头部认证信息")
    private String authHeader;

    @ApiModelProperty("推送方式:POST,GET")
    private String httpMethod;


}
