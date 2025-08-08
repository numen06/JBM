package com.jbm.cluster.api.constants.center;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import lombok.Getter;

/**
 * @author scolin
 * @description
 * @date 2025/7/24 10:13
 */
@Getter
@JbmDicType(typeName = "数据源类型" , value = "data_source_type")
public enum DataSourceType {
    customization("customization", "自定义选项"),
    http("http", "HTTP");
    @EnumValue
    private final String key;
    private final String value;
    DataSourceType(String key , String value) {
        this.key = key;
        this.value = value;
    }
}
