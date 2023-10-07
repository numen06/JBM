package com.jbm.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

public class Emptys {

    private Boolean result = false;


    public Emptys() {
    }

    public static Emptys create() {
        return new Emptys();
    }

    public Emptys and(Boolean... values) {
        for (Boolean v : values) {
            result = result && v;
        }
        return this;
    }

    public Emptys or(Boolean... values) {
        for (Boolean v : values) {
            result = result || v;
        }
        return this;
    }

    public Emptys isEmpty(Object obj) {
        return this.and(result, ObjectUtil.isEmpty(obj));
    }

    public Emptys orEmpty(Object obj) {
        return this.or(result, ObjectUtil.isEmpty(obj));
    }

    public Emptys orBlank(String str) {
        return this.or(StrUtil.isBlank(str));
    }

    public Boolean result() {
        return this.result;
    }

}
