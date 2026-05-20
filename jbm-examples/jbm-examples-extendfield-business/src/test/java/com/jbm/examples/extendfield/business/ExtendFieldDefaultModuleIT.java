package com.jbm.examples.extendfield.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.examples.extendfield.designer.ExtendFieldDesignerApplication;
import com.jbm.framework.metadata.bean.ResultBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 不传租户头时使用默认模块 tenant_id=0。
 */
@SpringBootTest(classes = ExtendFieldBusinessApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2-redis")
class ExtendFieldDefaultModuleIT {

    private static final String FORM_CODE = "default_module_form";

    private static ConfigurableApplicationContext designerContext;
    private static int designerPort;

    private final RestTemplate designerClient = new RestTemplate();

    @Autowired
    private TestRestTemplate businessClient;

    @BeforeAll
    static void startDesigner() {
        assumeTrue(isRedisReachable(
                System.getenv().getOrDefault("REDIS_HOST", "10.100.10.62"),
                Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"))));
        designerContext = SpringApplication.run(
                ExtendFieldDesignerApplication.class,
                "--spring.profiles.active=h2-redis",
                "--server.port=0",
                "--spring.application.name=extendfield-designer-default-it",
                "--spring.datasource.url=jdbc:h2:mem:designer_default_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "--spring.liquibase.change-log=classpath:db/extendfield-designer/db.changelog-master.yaml");
        designerPort = designerContext.getEnvironment().getProperty("local.server.port", Integer.class);
    }

    @AfterAll
    static void stopDesigner() {
        if (designerContext != null) {
            SpringApplication.exit(designerContext);
        }
    }

    @Test
    void noTenantHeader_usesDefaultModuleScope() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("formName", "默认模块表单");
        form.put("fields", Arrays.asList(
                mapDef("note", "string", "备注")));

        ResponseEntity<ResultBody> save = designerClient.exchange(
                "http://127.0.0.1:" + designerPort + "/api/designer/forms/" + FORM_CODE,
                HttpMethod.POST,
                jsonEntity(form),
                ResultBody.class);
        assertEquals(HttpStatus.OK, save.getStatusCode());

        Map<String, Object> order = new HashMap<>();
        order.put("formCode", FORM_CODE);
        order.put("orderNo", "DEF-001");
        order.put("title", "无租户头订单");
        order.put("note", "默认模块备注");

        ResponseEntity<ResultBody> created = businessClient.exchange(
                "/api/business/orders",
                HttpMethod.POST,
                jsonEntity(order),
                ResultBody.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        JSONObject row = JSON.parseObject(JSON.toJSONString(created.getBody().getResult()));
        assertEquals("默认模块备注", row.getString("note"));
        assertNull(row.get("extendData"));
    }

    private static Map<String, String> mapDef(String name, String type, String label) {
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
