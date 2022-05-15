package com.jbm.cluster.api.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备类型
 * 针对一套 用户体系
 *
 * @author Lion Li
 */
@JbmDicType(typeName = "区域类型")
public enum RequestDeviceType {

    /**
     * pc端
     */
    PC("pc"),

    /**
     * app端
     */
    APP("app"),

    /**
     * 小程序端
     */
    XCX("xcx");

    @EnumValue
    private final String key;
    private final String value;

    RequestDeviceType(String value) {
        this.value = value;
        this.key = this.toString();
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public String getDevice() {
        return this.value;
    }
}
