package com.jbm.dic.test;

import com.jbm.autoconfig.dic.EnumTypeConverter;
import com.jbm.framework.dictionary.JbmDictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnumTypeConverterTest {

    private final EnumTypeConverter converter = new EnumTypeConverter();

    @Test
    void convert_codeFieldNameDoesNotOverwriteDictionaryCode() {
        List<JbmDictionary> list = converter.convert(EnumCodeFieldConflictDict.class);
        assertNotNull(list);
        JbmDictionary first = list.get(0);
        assertEquals("stored-key", first.getCode(), "code 应来自 @EnumValue 字段 key，而非同名的 code 字段");
        assertEquals("展示一", first.getValue());
    }

    @Test
    void convert_valueFieldNameDoesNotOverwriteDictionaryValue() {
        List<JbmDictionary> list = converter.convert(EnumValueFieldConflictDict.class);
        assertNotNull(list);
        JbmDictionary first = list.get(0);
        assertEquals("c1", first.getCode());
        assertEquals("展示", first.getValue(), "value 应来自 @JbmDicValue 字段 name，而非同名的 value 字段");
    }

    @Test
    void convert_pileDealStatusDict2_regression() {
        List<JbmDictionary> list = converter.convert(PileDealStatusDict2.class);
        assertNotNull(list);
        assertEquals(6, list.size());
        JbmDictionary first = list.get(0);
        assertEquals("空闲中", first.getValue());
    }

    /**
     * code、value 均为中文：@EnumValue 指向的 key 与 value 字段应原样进入字典。
     */
    @Test
    void convert_chineseKeyAndChineseValue_withEnumValue() {
        List<JbmDictionary> list = converter.convert(EnumChineseKeyValueDict.class);
        assertNotNull(list);
        assertEquals(1, list.size());
        JbmDictionary d = list.get(0);
        assertEquals("存储编码壹", d.getCode(), "字典 code 应为 @EnumValue 字段（中文存储值）");
        assertEquals("展示标签壹", d.getValue(), "字典 value 应为展示字段（中文）");
    }

    /**
     * code、value 均为中文：仅 @JbmDicCode / @JbmDicValue 标注时也应正确识别。
     */
    @Test
    void convert_chineseCodeAndChineseName_withJbmDicAnnotations() {
        List<JbmDictionary> list = converter.convert(EnumChineseJbmDicDict.class);
        assertNotNull(list);
        assertEquals(1, list.size());
        JbmDictionary d = list.get(0);
        assertEquals("业务码甲", d.getCode());
        assertEquals("名称甲", d.getValue());
    }
}
