package com.jbm.cluster.weixin.miniapp.form;

import lombok.Data;

@Data
public class WxUserInfo {

    private String sessionKey;
    private String signature;
    private String rawData;
    private String encryptedData;
    private String iv;


}
