package com.jbm.framework.eventbus.test;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.util.ClassUtils;
import org.junit.jupiter.api.Test;

public class BeanNameTest {


    @Test
    public void test() {
        System.out.println(ClassUtils.getShortNameAsProperty(BeanNameTest.class));
    }
}
