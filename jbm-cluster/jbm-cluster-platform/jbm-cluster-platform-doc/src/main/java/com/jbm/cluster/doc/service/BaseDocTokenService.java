package com.jbm.cluster.doc.service;

import com.jbm.cluster.api.entitys.doc.BaseDocToken;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.Date;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-11-28 17:20:20
 */
public interface BaseDocTokenService extends IMasterDataService<BaseDocToken> {


    BaseDocToken createGroupToken(Date expirationTime,String groupId);


    BaseDocToken createGroupDayToken(String groupId);

    BaseDocToken checkToken(String tokenKey);
}
