package com.jbm.cluster.api.model.push;

import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushWay;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PushMsg {

    @ApiModelProperty("发送者ID")
    private Long sendUserId;

    @ApiModelProperty("应用")
    private Long appId;

    @ApiModelProperty("是否是系统消息")
    private Boolean sysMsg;

    @ApiModelProperty("推送消息类型")
    private PushMsgType pushMsgType;

    @ApiModelProperty("接收者ID")
    private List<Long> recUserIds;

    @ApiModelProperty("标签组")
    private String tags;

    @ApiModelProperty("通知方式")
    private List<PushWay> pushWays;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("内容")
    private String content;

    @ApiModelProperty("扩展信息")
    private Map<String, Object> extend;

    @ApiModelProperty("模板号")
    private String templateCode;

    @ApiModelProperty("模板参数")
    private Map<String, Object> templateData;

    @ApiModelProperty("消息等级")
    private Integer level;
}
