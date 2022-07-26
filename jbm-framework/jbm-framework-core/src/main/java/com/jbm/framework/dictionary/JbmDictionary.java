package com.jbm.framework.dictionary;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.io.Serializable;

@Data
public class JbmDictionary implements Serializable {


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
     * 类型的解释
     */
    private String typeName;

//    /**
//     * 其他参数
//     */
//    private Map<String, Object> values;

    public JbmDictionary() {
        super();
    }

//    public JbmDictionary(Map<String, Object> values) {
//        this.values = values;
//    }

    /**
     * 通过类型和CODE生成唯一ID
     *
     * @return
     */
    public long hashId() {
        final String temp = StrUtil.concat(true, application, code, value);
        return new Long(temp.hashCode()).longValue();
    }
}
