package com.jbm.cluster.api.model.message;

import lombok.Data;

import java.util.Map;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
@Data
public class SmsNotification implements Notification {

    private String phoneNumber;

    private String templateCode;

    private Map<String, Object> params;

    private String signName;

}
