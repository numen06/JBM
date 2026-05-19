package com.jbm.micro.mysql;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 扩展字段 REDIS 模式联调：连接 10.100.10.62:6379，启动同步 YAML 定义 → 写业务 → 校验 Redis Key。
 * <p>Redis 不可达时自动跳过（Assumptions）。</p>
 */
@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2-redis")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MicroMysqlExtendFieldRedisIT {

    private static final String REDIS_HOST = "10.100.10.62";
    private static final int REDIS_PORT = 6379;
    private static final String FORM_CODE = "extend_demo_form";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private RedisService redisService;

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Test
    @Order(1)
    void step01_redisReachableAndDefinitionsSynced() {
        assumeRedisUp();
        assertNotNull(redisService, "应加载 jbm-framework-autoconfigure-redis");

        String formKey = "extend_field:form:" + FORM_CODE;
        String namesKey = "extend_field:names:" + FORM_CODE;

        Map<String, Object> formMap = redisService.getCacheMap(formKey);
        Set<Object> names = redisService.getCacheSet(namesKey);
        Assumptions.assumeTrue(formMap != null && !formMap.isEmpty(),
                "启动后 Redis 中应有字段定义，请检查 sync-local-to-redis-on-startup");
        Assumptions.assumeTrue(names != null && !names.isEmpty());

        Set<String> nameSet = new HashSet<>();
        for (Object n : names) {
            if (n != null) {
                nameSet.add(n.toString());
            }
        }
        assertTrue(nameSet.contains("contactPhone"));
        assertTrue(nameSet.contains("region"));

        ResponseEntity<ResultBody> defs = restTemplate.getForEntity(
                "/api/extend-field/definitions/" + FORM_CODE, ResultBody.class);
        assertEquals(HttpStatus.OK, defs.getStatusCode());
        assertTrue(defs.getBody().getSuccess());
        JSONArray arr = JSON.parseArray(JSON.toJSONString(defs.getBody().getResult()));
        assertTrue(arr.size() >= 2);
    }

    @Test
    @Order(2)
    void step02_createWithFormCodeUsingRedisDefinitions() {
        assumeRedisUp();
        assertNotNull(fieldDefinitionService);
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("contactPhone"));

        Map<String, Object> create = new HashMap<>();
        create.put("formCode", FORM_CODE);
        create.put("bizCode", "EXT-REDIS-001");
        create.put("title", "Redis 定义联调");
        create.put("contactPhone", "13600000001");
        create.put("region", "西南");

        ResponseEntity<ResultBody> post = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.POST,
                jsonEntity(create),
                ResultBody.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertTrue(post.getBody().getSuccess());

        JSONObject row = JSON.parseObject(JSON.toJSONString(post.getBody().getResult()));
        assertEquals("13600000001", row.getString("contactPhone"));
        assertEquals("西南", row.getString("region"));
        assertNull(row.get("extendData"));
    }

    @Test
    @Order(3)
    void step03_searchAfterRedisDefinition() {
        assumeRedisUp();

        Map<String, Object> search = new HashMap<>();
        search.put("bizCode", "EXT-REDIS-001");
        search.put("extend", new HashMap<String, Object>() {{
            put("region", "西南");
        }});

        ResponseEntity<ResultBody> found = restTemplate.exchange(
                "/api/h2/mp/extend-demos/search",
                HttpMethod.POST,
                jsonEntity(search),
                ResultBody.class);
        assertEquals(HttpStatus.OK, found.getStatusCode());
        JSONArray rows = JSON.parseArray(JSON.toJSONString(found.getBody().getResult()));
        assertFalse(rows.isEmpty());
        assertEquals("西南", rows.getJSONObject(0).getString("region"));
    }

    private static void assumeRedisUp() {
        Assumptions.assumeTrue(isRedisReachable(REDIS_HOST, REDIS_PORT),
                () -> "跳过：无法连接 Redis " + REDIS_HOST + ":" + REDIS_PORT);
    }

    private static boolean isRedisReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
