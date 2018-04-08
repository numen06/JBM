package com.jbm.test.dubbo;

import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Service;

@Service
@Component
public class DubboProviderServcieImpl implements DubboProviderServcie {
	public void test() {
		System.out.println("wo shi test");
	}
}
