package com.jbm.util.oshi.info;

import lombok.Data;

@Data
public class SysInfo {

    //获取计算机系统，固件，底版--每天执行一次，或者手动执行一次
    private String manufacturer;
    private String serialNumber;
    private String hardwareUUID;
    //操作系统和版本/内部版本
    private String osVersion;
}
