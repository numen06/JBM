package com.jbm.examples.extendfield.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.examples.extendfield.designer.ExtendFieldDesignerApplication;
import com.jbm.examples.extendfield.business.web.filter.DemoTenantIdHeaderFilter;
import com.jbm.framework.metadata.bean.ResultBody;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 同一 formCode、不同租户：字段定义与业务拆分互不影响。
 */
@SpringBootTest(classes = ExtendFieldBusinessApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2-redis")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtendFieldDualTenantIT {

    private static final String FORM_CODE = "shared_sales_form";
    private static final String TENANT_A = "1001";
    private static final String TENANT_B = "2002";

    private static ConfigurableApplicationContext designerContext;
    private static int designerPort;

    private final RestTemplate designerClient = new RestTemplate();

    @Autowired
    private TestRestTemplate businessClient;

    @BeforeAll
    static void startDesignerApp() {
        assumeTrue(isRedisReachable(
                System.getenv().getOrDefault("REDIS_HOST", "10.100.10.62"),
                Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"))));
        designerContext = SpringApplication.run(
                ExtendFieldDesignerApplication.class,
                "--spring.profiles.active=h2-redis",
                "--server.port=0",
                "--spring.application.name=jbm-examples-extendfield-designer-tenant-it",
                "--spring.datasource.url=jdbc:h2:mem:extendfield_designer_tenant_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                "--spring.liquibase.change-log=classpath:db/extendfield-designer/db.changelog-master.yaml");
        designerPort = designerContext.getEnvironment().getProperty("local.server.port", Integer.class);
    }

    @AfterAll
    static void stopDesignerApp() {
        if (designerContext != null) {
            SpringApplication.exit(designerContext);
        }
    }

    @Test
    @Order(1)
    void sameFormCode_differentTenants_differentFieldDefinitions() {
        Map<String, Object> formA = new LinkedHashMap<>();
        formA.put("formName", "租户A表单");
        formA.put("fields", Arrays.asList(
                def("contactPhone", "string", "电话"),
                def("region", "string", "区域")));
        assertEquals(HttpStatus.OK, designerClient.exchange(
                designerUrl() + "/api/designer/forms/" + FORM_CODE,
                HttpMethod.POST, jsonEntity(formA, TENANT_A), ResultBody.class).getStatusCode());

        Map<String, Object> formB = new LinkedHashMap<>();
        formB.put("formName", "租户B表单");
        formB.put("fields", Arrays.asList(
                def("vipLevel", "string", "VIP"),
                def("industry", "string", "行业")));
        assertEquals(HttpStatus.OK, designerClient.exchange(
                designerUrl() + "/api/designer/forms/" + FORM_CODE,
                HttpMethod.POST, jsonEntity(formB, TENANT_B), ResultBody.class).getStatusCode());
    }

    @Test
    @Order(2)
    void tenantA_usesContactPhone_tenantB_usesVipLevel() {
        Map<String, Object> orderA = new HashMap<>();
        orderA.put("formCode", FORM_CODE);
        orderA.put("orderNo", "A-001");
        orderA.put("title", "租户A订单");
        orderA.put("contactPhone", "13800001111");
        orderA.put("region", "华东");
        orderA.put("vipLevel", "不应拆分");

        ResponseEntity<ResultBody> resA = businessClient.exchange(
                "/api/business/orders", HttpMethod.POST, jsonEntity(orderA, TENANT_A), ResultBody.class);
        assertEquals(HttpStatus.OK, resA.getStatusCode());
        JSONObject rowA = JSON.parseObject(JSON.toJSONString(resA.getBody().getResult()));
        assertEquals("13800001111", rowA.getString("contactPhone"));
        assertNull(rowA.getString("vipLevel"));

        Map<String, Object> orderB = new HashMap<>();
        orderB.put("formCode", FORM_CODE);
        orderB.put("orderNo", "B-001");
        orderB.put("title", "租户B订单");
        orderB.put("vipLevel", "金卡");
        orderB.put("industry", "制造");
        orderB.put("contactPhone", "不应拆分");

        ResponseEntity<ResultBody> resB = businessClient.exchange(
                "/api/business/orders", HttpMethod.POST, jsonEntity(orderB, TENANT_B), ResultBody.class);
        assertEquals(HttpStatus.OK, resB.getStatusCode());
        JSONObject rowB = JSON.parseObject(JSON.toJSONString(resB.getBody().getResult()));
        assertEquals("金卡", rowB.getString("vipLevel"));
        assertNull(rowB.getString("contactPhone"));
    }

    @Test
    @Order(3)
    void listOrders_isolatedByTenant() {
        ResponseEntity<ResultBody> listA = businessClient.exchange(
                "/api/business/orders", HttpMethod.GET, tenantEntity(TENANT_A), ResultBody.class);
        ResponseEntity<ResultBody> listB = businessClient.exchange(
                "/api/business/orders", HttpMethod.GET, tenantEntity(TENANT_B), ResultBody.class);
        assertEquals(HttpStatus.OK, listA.getStatusCode());
        assertEquals(HttpStatus.OK, listB.getStatusCode());
        String jsonA = JSON.toJSONString(listA.getBody().getResult());
        String jsonB = JSON.toJSONString(listB.getBody().getResult());
        assertTrue(jsonA.contains("A-001"));
        assertFalse(jsonA.contains("B-001"));
        assertTrue(jsonB.contains("B-001"));
        assertFalse(jsonB.contains("A-001"));
    }

    private String designerUrl() {
        return "http://127.0.0.1:" + designerPort;
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

    private static <T> HttpEntity<T> jsonEntity(T body, String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(DemoTenantIdHeaderFilter.HEADER_NAME, tenantId);
        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<Void> tenantEntity(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(DemoTenantIdHeaderFilter.HEADER_NAME, tenantId);
        return new HttpEntity<>(headers);
    }
}
