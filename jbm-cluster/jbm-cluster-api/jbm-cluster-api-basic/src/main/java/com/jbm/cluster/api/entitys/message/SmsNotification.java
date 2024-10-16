package com.jbm.cluster.api.entitys.message;

import lombok.Data;

import java.util.Map;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Data
public class SmsNotification extends Notification {

    private String phoneNumber;

    private String templateCode;

    private Map<String, Object> params;

    private String signName;

}
