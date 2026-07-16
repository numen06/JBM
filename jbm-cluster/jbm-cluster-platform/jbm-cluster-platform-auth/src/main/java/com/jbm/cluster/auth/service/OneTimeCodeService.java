package com.jbm.cluster.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Redis 一次性验证码存储。
 */
@Service
public class OneTimeCodeService {

    private static final DefaultRedisScript<String> GET_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); end; "
                    + "return value;",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public OneTimeCodeService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 原子读取并删除验证码，确保并发请求中最多只有一个请求可以消费成功。
     */
    public String consume(String key) {
        return stringRedisTemplate.execute(GET_AND_DELETE_SCRIPT, Collections.singletonList(key));
    }
}
