package com.jbm.cluster.push.service.impl;

import com.jbm.cluster.api.entitys.message.PushItem;
import com.jbm.cluster.push.service.PushItemService;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.service.mybatis.MasterDataTreeServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @Author: wesley.zhang
 * @Create: 2021-10-08 16:43:28
 */
@Service
public class PushItemServiceImpl extends MasterDataTreeServiceImpl<PushItem> implements PushItemService {


}