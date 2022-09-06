package com.jbm.cluster.api.entitys.message;

import com.jbm.cluster.api.entitys.message.weixin.MiniProgram;
import com.jbm.cluster.api.entitys.message.weixin.WxMpTemplateData;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Data
public class WeixinNotification extends Notification {

    @ApiModelProperty("接受者OPENID")
    private String toUser;
    @ApiModelProperty("模板ID")
    private String templateId;
    @ApiModelProperty("模板跳转链接（海外帐号没有跳转能力）")
    private String url;
    @ApiModelProperty("跳小程序所需数据，不需跳小程序可不用传该数据")
    private MiniProgram miniProgram;
    @ApiModelProperty("模板数据")
    private List<WxMpTemplateData> data;
//    @ApiModelProperty("模板内容字体颜色，不填默认为黑色")
//    private String color;
    @ApiModelProperty(value = "防重入id", notes = "对于同一个openid + client_msg_id, 只发送一条消息,10分钟有效,超过10分钟不保证效果。若无防重入需求，可不填")
    private String clientMsgId;

}
