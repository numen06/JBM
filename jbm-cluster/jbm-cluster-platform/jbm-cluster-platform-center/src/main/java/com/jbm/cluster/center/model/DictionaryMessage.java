package com.jbm.cluster.center.model;

import lombok.Data;

/**
 * 数据库中的字典国际化文案。
 */
@Data
public class DictionaryMessage {

    private String code;
    private String messageText;
    private String locale;
}
