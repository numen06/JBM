package com.jbm.cluster.center.service.impl;

import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.center.mapper.DictionaryMessageMapper;
import com.jbm.cluster.center.model.DictionaryMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DictionaryI18nServiceImplTest {

    private final DictionaryMessageMapper mapper = mock(DictionaryMessageMapper.class);
    private final DictionaryI18nServiceImpl service = new DictionaryI18nServiceImpl(mapper);

    @Test
    void usesExactEnglishTranslationWithoutMutatingCachedDictionary() {
        when(mapper.selectByLocales(anyCollection())).thenReturn(Collections.singletonList(
                message("dict.account_status.enabled", "en_US", "Enabled")));
        BaseDic original = dictionary("enabled", "启用");

        BaseDic localized = service.localize(dictionaries("account_status", original), Locale.US)
                .get("account_status").get(0);

        assertEquals("Enabled", localized.getName());
        assertEquals("启用", original.getName());
        assertNotSame(original, localized);
    }

    @Test
    void usesGenericEnglishAsDefaultForUnsupportedForeignLanguage() {
        when(mapper.selectByLocales(anyCollection())).thenReturn(Arrays.asList(
                message("dict.account_status.enabled", "zh", "启用（数据库）"),
                message("dict.account_status.enabled", "en", "Enabled")));

        BaseDic localized = service.localize(
                        dictionaries("account_status", dictionary("enabled", "启用")), Locale.FRANCE)
                .get("account_status").get(0);

        assertEquals("Enabled", localized.getName());
    }

    @Test
    void fallsBackToConfiguredChineseThenOriginalName() {
        when(mapper.selectByLocales(anyCollection())).thenReturn(Collections.singletonList(
                message("dict.account_status.enabled", "zh", "启用（数据库）")));
        Map<String, List<BaseDic>> source = new LinkedHashMap<>();
        source.put("account_status", Arrays.asList(
                dictionary("enabled", "启用"),
                dictionary("disabled", "停用")));

        List<BaseDic> localized = service.localize(source, Locale.JAPAN).get("account_status");

        assertEquals("启用（数据库）", localized.get(0).getName());
        assertEquals("停用", localized.get(1).getName());
    }

    @Test
    void prefersRequestedLanguageOverEnglishFallback() {
        when(mapper.selectByLocales(anyCollection())).thenReturn(Arrays.asList(
                message("dict.account_status.enabled", "en", "Enabled"),
                message("dict.account_status.enabled", "fr", "Activé")));

        BaseDic localized = service.localize(
                        dictionaries("account_status", dictionary("enabled", "启用")), Locale.FRANCE)
                .get("account_status").get(0);

        assertEquals("Activé", localized.getName());
    }

    private static Map<String, List<BaseDic>> dictionaries(String type, BaseDic dictionary) {
        Map<String, List<BaseDic>> result = new LinkedHashMap<>();
        result.put(type, Collections.singletonList(dictionary));
        return result;
    }

    private static BaseDic dictionary(String code, String name) {
        BaseDic dictionary = new BaseDic();
        dictionary.setCode(code);
        dictionary.setName(name);
        return dictionary;
    }

    private static DictionaryMessage message(String code, String locale, String text) {
        DictionaryMessage message = new DictionaryMessage();
        message.setCode(code);
        message.setLocale(locale);
        message.setMessageText(text);
        return message;
    }
}
