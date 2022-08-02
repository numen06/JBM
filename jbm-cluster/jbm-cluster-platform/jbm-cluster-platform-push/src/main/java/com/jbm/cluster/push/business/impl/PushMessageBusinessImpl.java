package com.jbm.cluster.push.business.impl;

import com.jbm.framework.masterdata.business.PlatformBusinessImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.jbm.cluster.push.business.PushMessageBusiness;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.cluster.push.service.PushMessageBodyService;

/**
 * 消息推送项业务实现类
 * @Author: jbm
 * @Create: 2022-08-01 17:40:30
 */
@Service
public class PushMessageBusinessImpl extends PlatformBusinessImpl implements PushMessageBusiness {

    @Autowired
    private PushMessageItemService pushMessageItemService;
    @Autowired
    private PushMessageBodyService pushMessageBodyService;
}
