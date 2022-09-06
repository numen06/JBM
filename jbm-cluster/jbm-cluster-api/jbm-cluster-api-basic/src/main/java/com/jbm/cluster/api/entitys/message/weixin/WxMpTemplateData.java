package com.jbm.cluster.api.entitys.message.weixin;

import lombok.Data;

@Data
public class WxMpTemplateData {
    private String name;
    private String value;
    private String color;

    public WxMpTemplateData() {
    }

    public WxMpTemplateData(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public WxMpTemplateData(String name, String value, String color) {
        this.name = name;
        this.value = value;
        this.color = color;
    }


}
