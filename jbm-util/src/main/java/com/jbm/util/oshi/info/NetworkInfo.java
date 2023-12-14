package com.jbm.util.oshi.info;

import lombok.Data;

import java.util.Date;

@Data
public class NetworkInfo {
    private String name;
    private String ips;
    private Double send;
    private String sendStr;
    private Double receive;
    private String receiveStr;
    private Date time;


}
