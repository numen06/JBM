package com.jbm.cluster.api.bo;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import lombok.Data;

@Data
public class PushMessage {

    private PushMessageBody pushMessageBody;

    private PushMessageItem pushMessageItem;
}
