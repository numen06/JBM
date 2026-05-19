package com.jbm.micro.mysql;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.micro.mysql.mp.MdExtendDemo;
import com.jbm.micro.mysql.service.MdExtendDemoService;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 与 {@code EXTEND_FIELD_TEST_CASES.md} 中 TC-EF-01～09 一一对应的全流程自动化。
 */
@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class MicroMysqlExtendFieldFullFlowIT {

    private static final String FORM_CODE = "extend_demo_form";
    private static final String BIZ_FORM = "EXT-FLOW-001";
    private static final String BIZ_EXPLICIT = "EXT-FLOW-002";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MdExtendDemoService mdExtendDemoService;

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Test
    void fullExtendFieldFlow_localMode() {
        // TC-EF-01
        assertNotNull(fieldDefinitionService);
        Set<String> names = fieldDefinitionService.getExtendFieldNames(FORM_CODE);
        assertTrue(names.contains("contactPhone"));
        assertTrue(names.contains("region"));

        ResponseEntity<ResultBody> defs = restTemplate.getForEntity(
                "/api/extend-field/definitions/" + FORM_CODE, ResultBody.class);
        assertEquals(HttpStatus.OK, defs.getStatusCode());
        assertTrue(defs.getBody().getSuccess());
        JSONArray defArr = JSON.parseArray(JSON.toJSONString(defs.getBody().getResult()));
        Set<String> fromApi = defArr.stream()
                .map(o -> ((JSONObject) o).getString("fieldName"))
                .collect(Collectors.toSet());
        assertTrue(fromApi.contains("contactPhone"));
        assertTrue(fromApi.contains("region"));

        // TC-EF-02
        Map<String, Object> createForm = new HashMap<>();
        createForm.put("formCode", FORM_CODE);
        createForm.put("bizCode", BIZ_FORM);
        createForm.put("title", "formCode 拆分创建");
        createForm.put("contactPhone", "13800138000");
        createForm.put("region", "华东");

        ResponseEntity<ResultBody> postForm = restTemplate.exchange(
                "/api/h2/mp/extend-demos", HttpMethod.POST, jsonEntity(createForm), ResultBody.class);
        assertEquals(HttpStatus.OK, postForm.getStatusCode());
        assertTrue(postForm.getBody().getSuccess());

        JSONObject createdForm = JSON.parseObject(JSON.toJSONString(postForm.getBody().getResult()));
        Long idFormCodeCreate = createdForm.getLong("id");
        assertNotNull(idFormCodeCreate);
        assertEquals(BIZ_FORM, createdForm.getString("bizCode"));
        assertEquals("13800138000", createdForm.getString("contactPhone"));
        assertEquals("华东", createdForm.getString("region"));
        assertNull(createdForm.get("extendData"));
        assertNull(createdForm.get("formCode"));

        MdExtendDemo dbForm = mdExtendDemoService.getById(idFormCodeCreate);
        assertEquals("13800138000", dbForm.getExtendData().get("contactPhone"));
        assertEquals("华东", dbForm.getExtendData().get("region"));

        // TC-EF-03
        ResponseEntity<ResultBody> getOne = restTemplate.getForEntity(
                "/api/h2/mp/extend-demos/" + idFormCodeCreate, ResultBody.class);
        assertEquals(HttpStatus.OK, getOne.getStatusCode());
        JSONObject got = JSON.parseObject(JSON.toJSONString(getOne.getBody().getResult()));
        assertEquals("华东", got.getString("region"));
        assertNull(got.get("extendData"));

        // TC-EF-04
        Map<String, Object> createExplicit = new HashMap<>();
        createExplicit.put("bizCode", BIZ_EXPLICIT);
        createExplicit.put("title", "直接 extendData");
        Map<String, Object> extend = new HashMap<>();
        extend.put("contactPhone", "13900001111");
        extend.put("region", "华南");
        createExplicit.put("extendData", extend);

        ResponseEntity<ResultBody> postExplicit = restTemplate.exchange(
                "/api/h2/mp/extend-demos", HttpMethod.POST, jsonEntity(createExplicit), ResultBody.class);
        assertEquals(HttpStatus.OK, postExplicit.getStatusCode());
        JSONObject createdExplicit = JSON.parseObject(JSON.toJSONString(postExplicit.getBody().getResult()));
        assertEquals("13900001111", createdExplicit.getString("contactPhone"));
        assertNull(createdExplicit.get("extendData"));

        MdExtendDemo dbExplicit = mdExtendDemoService.getById(createdExplicit.getLong("id"));
        assertEquals("华南", dbExplicit.getExtendData().get("region"));

        // TC-EF-05
        ResponseEntity<ResultBody<List>> list = restTemplate.exchange(
                "/api/h2/mp/extend-demos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResultBody<List>>() {
                });
        assertEquals(HttpStatus.OK, list.getStatusCode());
        JSONArray rows = JSON.parseArray(JSON.toJSONString(list.getBody().getResult()));
        boolean foundFlattened = false;
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (BIZ_FORM.equals(row.getString("bizCode"))) {
                assertNotNull(row.getString("region"));
                assertNull(row.get("extendData"));
                foundFlattened = true;
            }
        }
        assertTrue(foundFlattened);

        // TC-EF-06
        Map<String, Object> searchExtend = new HashMap<>();
        searchExtend.put("bizCode", BIZ_FORM);
        searchExtend.put("extend", new HashMap<String, Object>() {{
            put("region", "华东");
        }});
        JSONArray search1 = postSearch(searchExtend);
        assertEquals(1, search1.size());
        assertEquals(BIZ_FORM, search1.getJSONObject(0).getString("bizCode"));

        // TC-EF-07
        Map<String, Object> searchQuery = new HashMap<>();
        searchQuery.put("bizCode", BIZ_EXPLICIT);
        searchQuery.put("extendQuery", new HashMap<String, Object>() {{
            put("region", "华南");
        }});
        JSONArray search2 = postSearch(searchQuery);
        assertEquals(1, search2.size());

        // TC-EF-08
        Map<String, Object> searchMiss = new HashMap<>();
        searchMiss.put("bizCode", BIZ_FORM);
        searchMiss.put("extend", new HashMap<String, Object>() {{
            put("region", "不存在");
        }});
        JSONArray searchEmpty = postSearch(searchMiss);
        assertTrue(searchEmpty.isEmpty());

        // TC-EF-09
        Map<String, Object> saveDef = new HashMap<>();
        saveDef.put("formCode", "new_form");
        saveDef.put("definitions", java.util.Collections.singletonList(
                new HashMap<String, Object>() {{
                    put("fieldName", "x");
                    put("fieldType", "string");
                }}));
        ResponseEntity<ResultBody> saveDefResp = restTemplate.exchange(
                "/api/extend-field/definitions",
                HttpMethod.POST,
                jsonEntity(saveDef),
                ResultBody.class);
        assertEquals(HttpStatus.OK, saveDefResp.getStatusCode());
        assertFalse(saveDefResp.getBody().getSuccess());
        assertTrue(saveDefResp.getBody().getMessage().contains("Redis"));
    }

    private JSONArray postSearch(Map<String, Object> body) {
        ResponseEntity<ResultBody<List>> found = restTemplate.exchange(
                "/api/h2/mp/extend-demos/search",
                HttpMethod.POST,
                jsonEntity(body),
                new ParameterizedTypeReference<ResultBody<List>>() {
                });
        assertEquals(HttpStatus.OK, found.getStatusCode());
        return JSON.parseArray(JSON.toJSONString(found.getBody().getResult()));
    }

    private static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
