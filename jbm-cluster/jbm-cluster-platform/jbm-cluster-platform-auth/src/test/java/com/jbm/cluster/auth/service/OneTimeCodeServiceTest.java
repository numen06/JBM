package com.jbm.cluster.auth.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OneTimeCodeServiceTest {

    @Test
    void shouldConsumeCodeWithAtomicRedisScript() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), eq(java.util.Collections.singletonList("captcha-key"))))
                .thenReturn("123456");
        OneTimeCodeService service = new OneTimeCodeService(redisTemplate);

        assertEquals("123456", service.consume("captcha-key"));

        ArgumentCaptor<RedisScript> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(java.util.Collections.singletonList("captcha-key")));
        assertTrue(scriptCaptor.getValue().getScriptAsString().contains("redis.call('DEL', KEYS[1])"));
    }
}
