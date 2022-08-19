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
@ApiModel("Web反向推送")
public class WebhookTask extends MultiPlatformEntity {

    @ApiModelProperty("请求体")
    private String request;
    @ApiModelProperty("返回体")
    private String response;
    @ApiModelProperty("业务事件ID")
    private Integer httpStatus;
}
