package com.jbm.framework.eventbus.test;

import com.jbm.util.ClassUtils;
import org.junit.jupiter.api.Test;

public class BeanNameTest {


    @Test
    public void test() {
        System.out.println(ClassUtils.getShortNameAsProperty(BeanNameTest.class));
    }
}
