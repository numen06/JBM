package com.jbm.cluster.push.service;

import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
public interface PushMessageItemService extends IMasterDataService<PushMessageItem> {


    boolean read(List<String> ids);

    boolean unread(List<String> ids);

    boolean read(String id);

    boolean unread(String id);

    String toPush(PushWay pushWay, PushMessageBody pushMessageBody, Long recUserId);

    void sendCallBack(PushCallback pushCallback);

    DataPaging<PushMessageItem> findUserPushMessage(PushMessageForm pushMessageform);
}
