package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.model.push.PushMessageResult;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
public interface PushMessageBodyService extends IMasterDataService<PushMessageBody> {


    DataPaging<PushMessageBody> selectPageList(PageRequestBody pageRequestBody);

    DataPaging<PushMessageResult> selectPushMessagePageList(PushMessageForm pushMessageform);

    DataPaging<PushMessageResult> findUserPushMessage(PushMessageForm pushMessageform);

    /**
     * 非系统消息时解析发送者：未传 sendUserId 则取当前登录用户。
     * 须在 HTTP 请求线程调用（sendPushMsg 走 MQ 前需先执行）。
     */
    void prepareUserPushMsg(PushMsg pushMsg);

    void sendPushMsg(PushMsg pushMsg);

    void sendSysMessage(PushMessageBody pushMessageBody);

    void sendUserMessage(PushMessageBody pushMessageBody);
}
