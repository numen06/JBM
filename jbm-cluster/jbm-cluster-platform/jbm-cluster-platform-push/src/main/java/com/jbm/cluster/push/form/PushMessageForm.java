package com.jbm.cluster.push.form;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@ApiModel("站内信表单")
public class PushMessageForm extends PushMessageItem {

    private PushMessageBody pushMessageBody;
//    @ApiModelProperty("接收者ID")
//    private Long recUserId;
//    @ApiModelProperty("发送者ID")
//    private Long sendUserId;
//
    private PageForm pageForm;

    @ApiModelProperty("是否包含通讯测试消息")
    private Boolean includeTestMessages;

    @ApiModelProperty("消息类型")
    private PushMsgType type;

    @ApiModelProperty("来源类型：system,user")
    private String sourceType;

}
