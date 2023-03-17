package com.jbm.test.rest.bean;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-15 00:28
 **/
public enum DicEnumTest {

    cte(1, "tset"), dfasda(2, "test2");

    @EnumValue
    private int key;
    private String value;

    DicEnumTest(int key, String value) {
        this.key = key;
        this.value = value;
    }
}
