package com.jbm.util.oshi.info;

import lombok.Data;

import java.util.Date;

@Data
public class NetworkInfo {
    private String name;
    private String type;
    private String macAddr;
    private String ipV4Addr;
    private Double upload;
    private String uploadStr;
    private Double download;
    private String downloadStr;
    private Date time;


}
