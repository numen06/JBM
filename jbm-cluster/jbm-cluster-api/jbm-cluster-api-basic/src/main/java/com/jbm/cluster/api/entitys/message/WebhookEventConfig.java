package com.jbm.cluster.api.entitys.message;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName
@ApiModel("Web反向推送配置")
public class WebhookEventConfig extends MultiPlatformEntity {
    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    @ApiModelProperty(value = "事件ID")
    private String eventId;

    @ApiModelProperty("业务事件CODE")
    private String businessEventCode;

    @ApiModelProperty("事件名称")
    private String eventName;

    @ApiModelProperty("事件分组")
    private String eventGroup;

    @ApiModelProperty("事件默认内容:JSON")
    @Column(columnDefinition = "TEXT")
    private String eventBody;

    @ApiModelProperty("是否内部")
    private Boolean internal;

    @ApiModelProperty("服务名称")
    private String serviceName;

    @ApiModelProperty("是否启用")
    private Boolean enable;

    @ApiModelProperty("反向推送URL")
    private String url;

    @ApiModelProperty("头部认证信息")
    private String authHeader;

    @ApiModelProperty("推送方式:POST,GET")
    private String methodType;

    @ApiModelProperty("批次时间")
    private String batchTime;


}
