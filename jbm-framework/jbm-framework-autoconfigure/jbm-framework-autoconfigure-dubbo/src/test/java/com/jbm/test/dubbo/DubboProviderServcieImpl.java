package com.jbm.test.dubbo;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;

@Service
@Component
public class DubboProviderServcieImpl implements DubboProviderServcie {
    public void test() {
        System.out.println("wo shi test");
    }
}
