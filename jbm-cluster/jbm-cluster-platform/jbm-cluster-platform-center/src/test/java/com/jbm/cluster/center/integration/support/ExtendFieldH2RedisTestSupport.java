package com.jbm.cluster.center.integration.support;

import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 在 H2 集成测试中模拟 Redis 存取扩展字段定义。
 */
public abstract class ExtendFieldH2RedisTestSupport extends CenterH2ApiTestSupport {

    private final Map<String, Map<String, String>> formMaps = new ConcurrentHashMap<>();
    private final Map<String, Set<Object>> nameSets = new ConcurrentHashMap<>();

    @Autowired
    protected RedisService redisService;

    @BeforeEach
    void extendFieldRedisStub() {
        formMaps.clear();
        nameSets.clear();
        doAnswer(inv -> {
            formMaps.put(inv.getArgument(0), new HashMap<>(inv.getArgument(1)));
            return null;
        }).when(redisService).setCacheMap(anyString(), any());
        doAnswer(inv -> {
            nameSets.put(inv.getArgument(0), new HashSet<>(inv.getArgument(1)));
            return null;
        }).when(redisService).setCacheSet(anyString(), any());
        when(redisService.getCacheMap(anyString())).thenAnswer(inv ->
                formMaps.getOrDefault(inv.getArgument(0), Collections.emptyMap()));
        when(redisService.getCacheSet(anyString())).thenAnswer(inv ->
                nameSets.getOrDefault(inv.getArgument(0), Collections.emptySet()));
        doAnswer(inv -> {
            formMaps.remove(inv.getArgument(0));
            nameSets.remove(inv.getArgument(0));
            return null;
        }).when(redisService).deleteObject(anyString());
    }

    protected boolean redisContainsFormScope(String scopedFormCode) {
        return formMaps.containsKey("extend_field:form:" + scopedFormCode);
    }
}
