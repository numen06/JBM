package com.jbm.cluster.push.form;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
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


}
