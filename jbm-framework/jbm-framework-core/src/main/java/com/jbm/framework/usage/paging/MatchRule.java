package com.jbm.framework.usage.paging;

import lombok.Data;

import java.util.List;

/**
 * 匹配规则
 */
@Data
public class MatchRule {

    private String col;
    private String rule;
    private String val;
    private List<String> values;
}
