package com.jbm.framework.usage.paging;

public enum MatchRuleTypes {

    eq(2), in(-1), between(2), isNull(0), like(1);

    private int valNum;

    MatchRuleTypes(int valNum) {
        this.valNum = valNum;
    }

    public int getValNum() {
       return this.valNum;
    }


}
