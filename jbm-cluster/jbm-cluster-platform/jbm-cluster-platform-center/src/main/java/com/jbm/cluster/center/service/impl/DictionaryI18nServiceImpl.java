package com.jbm.cluster.center.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.center.mapper.DictionaryMessageMapper;
import com.jbm.cluster.center.model.DictionaryMessage;
import com.jbm.cluster.center.service.DictionaryI18nService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基于 prompt_message 的按需字典国际化实现。
 */
@Service
public class DictionaryI18nServiceImpl implements DictionaryI18nService {

    private static final String MESSAGE_PREFIX = "dict.";
    private static final String DEFAULT_LOCALE = Locale.CHINESE.toString();
    private static final String DEFAULT_ENGLISH_LOCALE = Locale.ENGLISH.toString();
    private static final String DEFAULT_ENGLISH_REGION_LOCALE = Locale.US.toString();

    private final DictionaryMessageMapper dictionaryMessageMapper;

    public DictionaryI18nServiceImpl(DictionaryMessageMapper dictionaryMessageMapper) {
        this.dictionaryMessageMapper = dictionaryMessageMapper;
    }

    @Override
    public Map<String, List<BaseDic>> localize(Map<String, List<BaseDic>> dictionaries, Locale locale) {
        if (dictionaries == null || dictionaries.isEmpty()) {
            return dictionaries;
        }

        List<String> localeCandidates = localeCandidates(locale);
        Map<String, Map<String, String>> messages = loadMessages(localeCandidates);
        Map<String, List<BaseDic>> result = Maps.newLinkedHashMap();

        dictionaries.forEach((typeCode, items) -> {
            List<BaseDic> localizedItems = Lists.newArrayList();
            if (items != null) {
                for (BaseDic item : items) {
                    if (item == null) {
                        continue;
                    }
                    BaseDic localizedItem = BeanUtil.copyProperties(item, BaseDic.class);
                    String messageCode = messageCode(typeCode, item.getCode());
                    localizedItem.setName(resolveMessage(messages, messageCode, localeCandidates, item.getName()));
                    localizedItems.add(localizedItem);
                }
            }
            result.put(typeCode, localizedItems);
        });
        return result;
    }

    private Map<String, Map<String, String>> loadMessages(List<String> localeCandidates) {
        Map<String, Map<String, String>> messages = Maps.newHashMap();
        List<DictionaryMessage> configuredMessages = dictionaryMessageMapper.selectByLocales(localeCandidates);
        if (configuredMessages == null) {
            return messages;
        }
        for (DictionaryMessage message : configuredMessages) {
            if (message == null || StrUtil.isBlank(message.getCode()) || StrUtil.isBlank(message.getLocale())
                    || StrUtil.isBlank(message.getMessageText())) {
                continue;
            }
            messages.computeIfAbsent(message.getCode(), ignored -> Maps.newHashMap())
                    .put(message.getLocale(), message.getMessageText());
        }
        return messages;
    }

    private String resolveMessage(Map<String, Map<String, String>> messages, String messageCode,
                                  List<String> localeCandidates, String defaultName) {
        Map<String, String> localizedMessages = messages.get(messageCode);
        if (localizedMessages == null) {
            return defaultName;
        }
        for (String localeCandidate : localeCandidates) {
            String message = localizedMessages.get(localeCandidate);
            if (StrUtil.isNotBlank(message)) {
                return message;
            }
        }
        return defaultName;
    }

    static String messageCode(String typeCode, String itemCode) {
        return MESSAGE_PREFIX + typeCode + "." + itemCode;
    }

    static List<String> localeCandidates(Locale locale) {
        Set<String> candidates = new LinkedHashSet<>();
        Locale requested = locale == null ? Locale.CHINESE : locale;
        addLocale(candidates, requested);

        // 中文请求回退到数据库现有的 zh；其他语言未配置时默认尝试英文。
        if (!Locale.CHINESE.getLanguage().equalsIgnoreCase(requested.getLanguage())) {
            candidates.add(DEFAULT_ENGLISH_LOCALE);
            candidates.add(DEFAULT_ENGLISH_REGION_LOCALE);
            candidates.add(Locale.US.toLanguageTag());
        }
        candidates.add(DEFAULT_LOCALE);
        return new ArrayList<>(candidates);
    }

    private static void addLocale(Set<String> candidates, Locale locale) {
        if (StrUtil.isNotBlank(locale.toString())) {
            candidates.add(locale.toString());
        }
        if (StrUtil.isNotBlank(locale.toLanguageTag()) && !"und".equals(locale.toLanguageTag())) {
            candidates.add(locale.toLanguageTag());
        }
        if (StrUtil.isNotBlank(locale.getLanguage())) {
            candidates.add(locale.getLanguage());
        }
    }
}
