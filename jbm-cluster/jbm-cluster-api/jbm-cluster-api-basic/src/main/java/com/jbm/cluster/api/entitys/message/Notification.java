package com.jbm.cluster.api.entitys.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jbm.cluster.api.constants.push.PushStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 03:52
 **/
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class Notification implements Serializable {

    @ApiModelProperty(value = "推送消息ID")
    private String msgId;
    @ApiModelProperty("发送状态")
    private PushStatus pushStatus;
}
