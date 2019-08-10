package com.jbm.autoconfig.dic;

import lombok.Data;

import java.util.Map;

@Data
public class JbmDictionary {


    /**
     * 系统
     */
    private String application;
    /**
     * 编码
     */
    private String code;
    /**
     * 名称
     */
    private String value;

    /**
     * 分组，分类，命名空间
     */
    private String type;

    /**
     * 其他参数
     */
    private Map<String, Object> values;

    public JbmDictionary() {
        super();
    }

    public JbmDictionary(Map<String, Object> values) {
        this.values = values;
    }

}
