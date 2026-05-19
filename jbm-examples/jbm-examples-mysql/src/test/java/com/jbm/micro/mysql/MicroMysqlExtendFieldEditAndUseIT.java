package com.jbm.micro.mysql;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.micro.mysql.web.FormDesignerController;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import jbm.framework.boot.autoconfigure.redis.RedisService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 模拟：配置侧「动态编辑表单」→ 定义入库 → 发布 Redis；业务侧只读 Redis 使用 formCode。
 */
@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2-redis")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MicroMysqlExtendFieldEditAndUseIT {

    private static final String FORM_CODE = "dynamic_sales_form";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Autowired(required = false)
    private RedisService redisService;

    @Test
    @Order(1)
    void step01_designerCreatesForm_persistDbAndRedis() {
        assumeRedisUp();

        FormDesignerController.SaveFormRequest body = new FormDesignerController.SaveFormRequest();
        body.setFormName("销售扩展表单 v1");
        body.setFields(Arrays.asList(
                def("contactPhone", "string", "联系电话"),
                def("region", "string", "区域")));

        ResponseEntity<ResultBody> save = restTemplate.exchange(
                "/api/h2/form-designer/forms/" + FORM_CODE,
                HttpMethod.POST,
                jsonEntity(body),
                ResultBody.class);
        assertEquals(HttpStatus.OK, save.getStatusCode());
        assertTrue(save.getBody().getSuccess());

        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("contactPhone"));
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("region"));
    }

    @Test
    @Order(2)
    void step02_businessUsesFormFromRedis() {
        assumeRedisUp();

        Map<String, Object> create = new HashMap<>();
        create.put("formCode", FORM_CODE);
        create.put("bizCode", "SALE-001");
        create.put("title", "第一单");
        create.put("contactPhone", "13611112222");
        create.put("region", "华东");

        ResponseEntity<ResultBody> post = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.POST,
                jsonEntity(create),
                ResultBody.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        JSONObject row = JSON.parseObject(JSON.toJSONString(post.getBody().getResult()));
        assertEquals("13611112222", row.getString("contactPhone"));
        assertNull(row.get("extendData"));
    }

    @Test
    @Order(3)
    void step03_designerAddsField_onlineEdit() {
        assumeRedisUp();

        FormDesignerController.UpdateFormRequest body = new FormDesignerController.UpdateFormRequest();
        body.setFormName("销售扩展表单 v2");
        body.setFields(Arrays.asList(
                def("contactPhone", "string", "联系电话"),
                def("region", "string", "区域"),
                def("vipLevel", "string", "VIP等级")));

        ResponseEntity<ResultBody> put = restTemplate.exchange(
                "/api/h2/form-designer/forms/" + FORM_CODE,
                HttpMethod.PUT,
                jsonEntity(body),
                ResultBody.class);
        assertEquals(HttpStatus.OK, put.getStatusCode());
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("vipLevel"));
    }

    @Test
    @Order(4)
    void step04_businessUsesNewFieldAfterEdit() {
        assumeRedisUp();

        Map<String, Object> create = new HashMap<>();
        create.put("formCode", FORM_CODE);
        create.put("bizCode", "SALE-002");
        create.put("title", "第二单");
        create.put("contactPhone", "13633334444");
        create.put("region", "华南");
        create.put("vipLevel", "金卡");

        ResponseEntity<ResultBody> post = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.POST,
                jsonEntity(create),
                ResultBody.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        JSONObject row = JSON.parseObject(JSON.toJSONString(post.getBody().getResult()));
        assertEquals("金卡", row.getString("vipLevel"));
    }

    @Test
    @Order(5)
    void step05_redisCleared_reloadFromDbThenBusinessStillWorks() {
        assumeRedisUp();
        assertNotNull(redisService);

        redisService.deleteObject("extend_field:form:" + FORM_CODE);
        redisService.deleteObject("extend_field:names:" + FORM_CODE);

        ResponseEntity<ResultBody> publish = restTemplate.exchange(
                "/api/h2/form-designer/forms/" + FORM_CODE + "/publish",
                HttpMethod.POST,
                jsonEntity(new HashMap<>()),
                ResultBody.class);
        assertEquals(HttpStatus.OK, publish.getStatusCode());
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("vipLevel"));

        Map<String, Object> create = new HashMap<>();
        create.put("formCode", FORM_CODE);
        create.put("bizCode", "SALE-003");
        create.put("title", "Redis 恢复后");
        create.put("vipLevel", "银卡");
        create.put("contactPhone", "13655556666");
        create.put("region", "华北");

        ResponseEntity<ResultBody> post = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.POST,
                jsonEntity(create),
                ResultBody.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertEquals("银卡", JSON.parseObject(JSON.toJSONString(post.getBody().getResult())).getString("vipLevel"));
    }

    private static FieldDefinition def(String name, String type, String label) {
        FieldDefinition f = new FieldDefinition();
        f.setFieldName(name);
        f.setFieldType(type);
        f.setFieldLabel(label);
        return f;
    }

    private static void assumeRedisUp() {
        assumeTrue(isRedisReachable("10.100.10.62", 6379));
    }

    private static boolean isRedisReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static <T> HttpEntity<T> jsonEntity(T body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
