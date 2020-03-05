package com.jbm.cluster.push.service;

import com.jbm.cluster.api.model.entity.message.PushMessage;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-05 03:05:23
 */
public interface PushMessageService extends IMasterDataService<PushMessage> {
    DataPaging<PushMessage> selectPageList(PageRequestBody pageRequestBody);

    boolean read(Long id);
}
