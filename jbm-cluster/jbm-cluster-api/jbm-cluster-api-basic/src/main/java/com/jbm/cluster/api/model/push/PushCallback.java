package com.jbm.cluster.api.model.push;

import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.constants.push.PushWay;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author wesley
 */delProperty("消息ID")
private String msgId;
@ApiModelProperty("错误消息内容")
private String errorMsg;
@ApiModelProperty("错误消息码")
private String errorCode;
@ApiModelProperty("发送状态")
private PushStatus pushStatus;
@ApiModelProperty("发送渠道")
private PushWay pushWay;
@Data
public class PushCallback implements Serializable {

    @ApiMo

}
