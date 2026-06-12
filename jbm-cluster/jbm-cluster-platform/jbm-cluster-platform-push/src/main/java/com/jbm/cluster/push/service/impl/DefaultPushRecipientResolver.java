package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.service.PushRecipientResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DefaultPushRecipientResolver implements PushRecipientResolver {

    @Override
    public Set<Long> resolve(PushMsg pushMsg) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (pushMsg == null) {
            return userIds;
        }
        if (CollUtil.isNotEmpty(pushMsg.getRecUserIds())) {
            userIds.addAll(pushMsg.getRecUserIds());
        }
        userIds.addAll(resolveTags(pushMsg.getTags()));
        userIds.remove(null);
        return userIds;
    }

    private Set<Long> resolveTags(String tags) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (StrUtil.isBlank(tags)) {
            return userIds;
        }
        List<String> parts = StrUtil.splitTrim(tags.replace(';', ',').replace(' ', ','), ',');
        for (String part : parts) {
            parseTag(part, userIds);
        }
        return userIds;
    }

    private void parseTag(String rawTag, Set<Long> userIds) {
        if (StrUtil.isBlank(rawTag)) {
            return;
        }
        String tag = rawTag.trim();
        if (StrUtil.startWithIgnoreCase(tag, "user:")) {
            parseUserIds(StrUtil.subAfter(tag, ":", false), userIds);
            return;
        }
        if (StrUtil.startWithIgnoreCase(tag, "users:")) {
            parseUserIds(StrUtil.subAfter(tag, ":", false), userIds);
            return;
        }
        if (StrUtil.isNumeric(tag)) {
            userIds.add(Long.valueOf(tag));
            return;
        }
        log.warn("暂未配置标签[{}]的用户解析规则，已跳过", tag);
    }

    private void parseUserIds(String value, Set<Long> userIds) {
        for (String item : StrUtil.splitTrim(value, '|')) {
            if (StrUtil.contains(item, ",")) {
                parseUserIds(item.replace(',', '|'), userIds);
                continue;
            }
            if (StrUtil.isNumeric(item)) {
                userIds.add(Long.valueOf(item));
            }
        }
    }
}
