package com.jbm.micro.mysql;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.micro.mysql.mp.MdExtendDemo;
import com.jbm.micro.mysql.service.MdExtendDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 扩展字段：formCode 拆分写入 extend_data、ResultBody 响应平铺、extend 条件查询。
 */
@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class MicroMysqlExtendFieldIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MdExtendDemoService mdExtendDemoService;

    @Test
    void createWithFormCodeSplitsExtendDataIntoDatabase() {
        Map<String, Object> body = new HashMap<>();
        body.put("formCode", "extend_demo_form");
        body.put("bizCode", "EXT-001");
        body.put("title", "扩展字段联调");
        body.put("contactPhone", "13800138000");
        body.put("region", "华东");

        HttpEntity<Map<String, Object>> request = jsonEntity(body);
        ResponseEntity<ResultBody> post = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.POST,
                request,
                ResultBody.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());

        JSONObject created = JSON.parseObject(JSON.toJSONString(post.getBody().getResult()));
        assertNotNull(created.getLong("id"));
        assertEquals("EXT-001", created.getString("bizCode"));
        assertEquals("13800138000", created.getString("contactPhone"));
        assertEquals("华东", created.getString("region"));
        assertNull(created.get("extendData"));

        Long id = created.getLong("id");
        MdExtendDemo fromDb = mdExtendDemoService.getById(id);
        assertNotNull(fromDb.getExtendData());
        assertEquals("13800138000", fromDb.getExtendData().get("contactPhone"));
        assertEquals("华东", fromDb.getExtendData().get("region"));
    }

    @Test
    void createWithExplicitExtendDataPersistsJsonColumn() {
        Map<String, Object> body = new HashMap<>();
        body.put("bizCode", "EXT-002");
        body.put("title", "直接 extendData");
        Map<String, Object> extend = new HashMap<>();
        extend.put("contactPhone", "13900001111");
        extend.put("region", "华南");
        body.put("extendData", extend);

        ResponseEntity<ResultBody> post = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.POST,
                jsonEntity(body),
                ResultBody.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());

        JSONObject created = JSON.parseObject(JSON.toJSONString(post.getBody().getResult()));
        Long id = created.getLong("id");
        MdExtendDemo fromDb = mdExtendDemoService.getById(id);
        assertEquals("13900001111", fromDb.getExtendData().get("contactPhone"));
    }

    @Test
    void searchByExtendCriteria() {
        Map<String, Object> create = new HashMap<>();
        create.put("formCode", "extend_demo_form");
        create.put("bizCode", "EXT-SRCH");
        create.put("title", "检索用");
        create.put("contactPhone", "13700000000");
        create.put("region", "华北");
        restTemplate.exchange("/api/h2/mp/extend-demos", HttpMethod.POST, jsonEntity(create), ResultBody.class);

        Map<String, Object> search = new HashMap<>();
        search.put("bizCode", "EXT-SRCH");
        Map<String, Object> extend = new HashMap<>();
        extend.put("region", "华北");
        search.put("extend", extend);

        ResponseEntity<ResultBody<List>> found = restTemplate.exchange(
                "/api/h2/mp/extend-demos/search",
                HttpMethod.POST,
                jsonEntity(search),
                new ParameterizedTypeReference<ResultBody<List>>() {
                });
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertNotNull(found.getBody());
        assertFalse(found.getBody().getResult().isEmpty());
    }

    private static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
