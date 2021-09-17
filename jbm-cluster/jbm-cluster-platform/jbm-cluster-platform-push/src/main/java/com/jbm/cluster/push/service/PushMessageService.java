package com.jbm.cluster.push.service;

import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-05 03:05:23
 */
public interface PushMessageService extends IMasterDataService<PushMessage> {


    DataPaging<PushMessage> selectPageList(PageRequestBody pageRequestBody);

    boolean read(List<Long> ids);

    boolean unread(List<Long> ids);

    boolean read(Long id);

    boolean unread(Long id);

    void sendSysMessage(PushMessage pushMessage);

    void sendUserMessage(PushMessage pushMessage);
}
