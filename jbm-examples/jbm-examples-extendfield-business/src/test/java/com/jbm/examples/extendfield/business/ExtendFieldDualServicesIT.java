package com.jbm.examples.extendfield.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.examples.extendfield.designer.ExtendFieldDesignerApplication;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 双进程联动：JUnit 内拉起设计器应用 + 本模块业务应用；设计器入库并发 Redis，业务只读 Redis。
 * <p>运行前需先 {@code mvn install -pl jbm-examples/jbm-examples-extendfield-designer -DskipTests}。</p>
 */
@SpringBootTest(classes = ExtendFieldBusinessApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2-redis")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtendFieldDualServicesIT {

    private static final String FORM_CODE = "dual_sales_form";

    private static ConfigurableApplicationContext designerContext;
    private static int designerPort;

    private final RestTemplate designerClient = new RestTemplate();

    @Autowired
    private TestRestTemplate businessClient;

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Autowired(required = false)
    private RedisService redisService;

    @BeforeAll
    static void startDesignerApp() {
        assumeTrue(isRedisReachable(
                System.getenv().getOrDefault("REDIS_HOST", "10.100.10.62"),
                Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"))));
        designerContext = SpringApplication.run(
                ExtendFieldDesignerApplication.class,
                "--spring.profiles.active=h2-redis",
                "--server.port=0",
                "--spring.application.name=jbm-examples-extendfield-designer-it",
                "--spring.datasource.url=jdbc:h2:mem:extendfield_designer_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                "--spring.liquibase.change-log=classpath:db/extendfield-designer/db.changelog-master.yaml");
        designerPort = designerContext.getEnvironment().getProperty("local.server.port", Integer.class);
    }

    @AfterAll
    static void stopDesignerApp() {
        if (designerContext != null) {
            SpringApplication.exit(designerContext);
            designerContext = null;
        }
    }

    private String designerBaseUrl() {
        return "http://127.0.0.1:" + designerPort;
    }

    @Test
    @Order(1)
    void designerCreatesForm_businessReadsFromRedis() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("formName", "双服务销售表单 v1");
        body.put("fields", Arrays.asList(
                def("contactPhone", "string", "联系电话"),
                def("region", "string", "区域")));

        ResponseEntity<ResultBody> save = designerClient.exchange(
                designerBaseUrl() + "/api/designer/forms/" + FORM_CODE,
                HttpMethod.POST,
                jsonEntity(body),
                ResultBody.class);
        assertEquals(HttpStatus.OK, save.getStatusCode());
        assertTrue(save.getBody().getSuccess());

        assertNotNull(fieldDefinitionService);
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("contactPhone"));

        Map<String, Object> order = new HashMap<>();
        order.put("formCode", FORM_CODE);
        order.put("orderNo", "ORD-001");
        order.put("title", "第一单");
        order.put("contactPhone", "13800001111");
        order.put("region", "华东");

        ResponseEntity<ResultBody> created = businessClient.exchange(
                "/api/business/orders",
                HttpMethod.POST,
                jsonEntity(order),
                ResultBody.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        JSONObject row = JSON.parseObject(JSON.toJSONString(created.getBody().getResult()));
        assertEquals("13800001111", row.getString("contactPhone"));
        assertNull(row.get("extendData"));
    }

    @Test
    @Order(2)
    void designerAddsField_businessUsesNewField() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("formName", "双服务销售表单 v2");
        body.put("fields", Arrays.asList(
                def("contactPhone", "string", "联系电话"),
                def("region", "string", "区域"),
                def("vipLevel", "string", "VIP等级")));

        ResponseEntity<ResultBody> put = designerClient.exchange(
                designerBaseUrl() + "/api/designer/forms/" + FORM_CODE,
                HttpMethod.PUT,
                jsonEntity(body),
                ResultBody.class);
        assertEquals(HttpStatus.OK, put.getStatusCode());
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("vipLevel"));

        Map<String, Object> order = new HashMap<>();
        order.put("formCode", FORM_CODE);
        order.put("orderNo", "ORD-002");
        order.put("title", "第二单");
        order.put("contactPhone", "13800002222");
        order.put("region", "华南");
        order.put("vipLevel", "金卡");

        ResponseEntity<ResultBody> created = businessClient.exchange(
                "/api/business/orders",
                HttpMethod.POST,
                jsonEntity(order),
                ResultBody.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        assertEquals("金卡", JSON.parseObject(JSON.toJSONString(created.getBody().getResult())).getString("vipLevel"));
    }

    @Test
    @Order(3)
    void redisCleared_designerRepublish_businessStillWorks() {
        assertNotNull(redisService);
        redisService.deleteObject("extend_field:form:" + FORM_CODE);
        redisService.deleteObject("extend_field:names:" + FORM_CODE);

        ResponseEntity<ResultBody> publish = designerClient.exchange(
                designerBaseUrl() + "/api/designer/forms/" + FORM_CODE + "/publish",
                HttpMethod.POST,
                jsonEntity(new HashMap<>()),
                ResultBody.class);
        assertEquals(HttpStatus.OK, publish.getStatusCode());
        assertTrue(fieldDefinitionService.getExtendFieldNames(FORM_CODE).contains("vipLevel"));

        Map<String, Object> order = new HashMap<>();
        order.put("formCode", FORM_CODE);
        order.put("orderNo", "ORD-003");
        order.put("title", "Redis 恢复后");
        order.put("vipLevel", "银卡");
        order.put("contactPhone", "13800003333");
        order.put("region", "华北");

        ResponseEntity<ResultBody> created = businessClient.exchange(
                "/api/business/orders",
                HttpMethod.POST,
                jsonEntity(order),
                ResultBody.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        assertEquals("银卡", JSON.parseObject(JSON.toJSONString(created.getBody().getResult())).getString("vipLevel"));
    }

    private static Map<String, String> def(String name, String type, String label) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("fieldName", name);
        f.put("fieldType", type);
        f.put("fieldLabel", label);
        return f;
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
