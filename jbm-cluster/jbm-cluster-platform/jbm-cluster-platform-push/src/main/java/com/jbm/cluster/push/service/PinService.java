package com.jbm.cluster.push.service;

import com.alibaba.fastjson.JSONObject;

/**
 * @author: create by wesley
 * @date:2019/4/10
 */
public interface PinService {

    JSONObject sendPinCode(String phoneNumber) throws Exception;

    Boolean vifCode(String phoneNumber, String code);
}
