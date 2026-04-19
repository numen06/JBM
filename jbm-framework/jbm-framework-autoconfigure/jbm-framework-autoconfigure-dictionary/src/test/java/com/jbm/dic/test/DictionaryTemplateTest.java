package com.jbm.dic.test;

import com.jbm.autoconfig.dic.DictionaryScanner;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.framework.dictionary.JbmDictionary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class DictionaryTemplateTest {

    @Test
    void getValuesByClassUsesJbmDicTypeValueAsCacheKey() {
        DictionaryScanner scanner = Mockito.mock(DictionaryScanner.class);
        Map<String, List<JbmDictionary>> cache = new ConcurrentHashMap<>();
        JbmDictionary d = new JbmDictionary();
        d.setCode("only");
        d.setValue("名称");
        cache.put("custom_dic_type_key", Collections.singletonList(d));
        when(scanner.getJbmDicMapCache()).thenReturn(cache);

        DictionaryTemplate template = new DictionaryTemplate(scanner);
        List<JbmDictionary> list = template.getValues(EnumForDictionaryTemplateTest.class);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("only", list.get(0).getCode());
    }
}
