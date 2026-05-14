package com.jbm.dic.test;

import com.jbm.autoconfig.dic.EnumTypeConverter;
import com.jbm.framework.dictionary.JbmDictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 复制 {@code AccountStatus} 结构，用 {@link EnumTypeConverter} 实际跑一遍并打印结果。
 */
class AccountStatusCopyConverterRunTest {

    @Test
    void run_convert_printsAndMatchesExpected() {
        EnumTypeConverter converter = new EnumTypeConverter();
        List<JbmDictionary> list = converter.convert(AccountStatusCopy.class);

        System.out.println("=== EnumTypeConverter.convert(AccountStatusCopy) ===");
        for (JbmDictionary d : list) {
            System.out.println("type=" + d.getType() + " typeName=" + d.getTypeName()
                    + " code=" + d.getCode() + " value=" + d.getValue());
        }

        assertEquals(3, list.size());

        assertEquals("AccountStatusCopy", list.get(0).getType());
        assertEquals("账号状态", list.get(0).getTypeName());

        assertEquals("0", list.get(0).getCode());
        assertEquals("禁用", list.get(0).getValue());
        assertEquals("1", list.get(1).getCode());
        assertEquals("正常", list.get(1).getValue());
        assertEquals("2", list.get(2).getCode());
        assertEquals("锁定", list.get(2).getValue());
    }
}
