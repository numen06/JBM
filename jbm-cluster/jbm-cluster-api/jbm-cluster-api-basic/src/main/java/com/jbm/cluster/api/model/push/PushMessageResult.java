package com.jbm.cluster.api.model.push;

import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.constants.push.PushWay;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
public class PushMessageResult implements Serializable {

    @ApiModelProperty(value = "推送消息ID")
    private String msgId;
    @ApiModelProperty("发送状态")
    private PushStatus pushStatus;
    @ApiModelProperty("发送渠道")
    private PushWay pushWay;
    @ApiModelProperty("是否已读")
    private Boolean readFlag;
    @ApiModelProperty("内容")
    private Object content;
    @ApiModelProperty("标题")
    private String title;
    @ApiModelProperty("等级")
    private Integer level;
    @ApiModelProperty("消息类型")
    private PushMsgType type;
//    @ApiModelProperty("扩展字段")
//    private Map<String, Object> extend;


}
