package com.jbm.dic.test;

import com.alibaba.fastjson.JSON;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.autoconfig.dic.EnumTypeConverter;
import com.jbm.autoconfig.dic.annotation.EnableJbmDictionary;
import jbm.framework.boot.autoconfigure.dictionary.DictionaryAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@EnableJbmDictionary
@SpringBootTest(classes = {DictionaryAutoConfiguration.class})
@Slf4j
public class DicTest {

    @Autowired
    private DictionaryTemplate dictionaryTemplate;

    /**
     * 测试枚举类型
     */
    @Test
    public void exampleTest3() {
        log.info("{}", dictionaryTemplate.getValues(PileDealStatusDict2.class));
    }


    /**
     * 测试枚举类型
     */
    @Test
    public void exampleTest4() {
        EnumTypeConverter enumTypeConverter = new EnumTypeConverter();
        System.out.println(JSON.toJSONString(enumTypeConverter.convert(PileDealStatusDict2.class)));
//		System.out.println(dictionaryCache.getValues("test"));
    }

    /**
     * 测试注解枚举
     */
    @Test
    public void exampleTest5() {
        EnumTypeConverter enumTypeConverter = new EnumTypeConverter();
        System.out.println(JSON.toJSONString(enumTypeConverter.convert(PileDealStatusDict3.class)));
//		System.out.println(dictionaryCache.getValues("test"));
    }


}
