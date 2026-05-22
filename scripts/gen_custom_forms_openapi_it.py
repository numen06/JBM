# -*- coding: utf-8 -*-
from pathlib import Path

TARGET = Path(
    r"d:\workspaces\JBM7\jbm-cluster\jbm-cluster-platform\jbm-cluster-platform-center"
    r"\src\test\java\com\jbm\cluster\center\integration\CustomFormsOpenApiH2IT.java"
)

JAVA = r'''package com.jbm.cluster.center.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jbm.cluster.api.constants.center.ComponentType;
import com.jbm.cluster.api.constants.center.FieldType;
import com.jbm.cluster.api.constants.center.FormOrTable;
import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.cluster.api.result.CustomFormsResult;
import com.jbm.cluster.center.integration.support.ExtendFieldH2RedisTestSupport;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomFormsOpenApiH2IT extends ExtendFieldH2RedisTestSupport {

    private static final String EXTEND_FORM_BASE = "/extend-field/forms";
    private static final String CUSTOM_FORMS_BASE = "/customForms";

    private static String sharedFormCode;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private static String formCode() {
        if (sharedFormCode == null) {
            sharedFormCode = "it_openapi_form_" + System.nanoTime();
        }
        return sharedFormCode;
    }

    @Test
    @Order(1)
    @DisplayName("Flow 1: Save extend form definition and publish to Redis")
    void testFlow1_saveExtendFormDefinition() throws Exception {
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("OpenAPI Extend Form");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("note");
        field.setFieldLabel("Note");
        field.setFieldType("string");
        request.setFields(Collections.singletonList(field));

        String code = formCode();
        MvcResult result = mockMvc.perform(post(EXTEND_FORM_BASE + "/{formCode}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.formCode").value(code))
                .andReturn();

        ResultBody<ExtendFormDefinition> body = parseResult(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<ExtendFormDefinition>>() {});
        assertSuccess(body);
        assertThat(body.getResult().getFields()).hasSize(1);
        assertThat(redisContainsFormScope("0:" + code)).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Flow 2: Get extend form definition from DB")
    void testFlow2_getExtendFormFromDb() throws Exception {
        String code = formCode();
        MvcResult result = mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.formCode").value(code))
                .andReturn();

        ResultBody<ExtendFormDefinition> body = parseResult(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<ExtendFormDefinition>>() {});
        assertSuccess(body);
        assertThat(body.getResult().getFormName()).isEqualTo("OpenAPI Extend Form");
    }

    @Test
    @Order(3)
    @DisplayName("Flow 3: List field definitions from Redis")
    void testFlow3_listDefinitionsFromRedis() throws Exception {
        String code = formCode();
        MvcResult result = mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}/definitions", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ResultBody<List<FieldDefinition>> body = parseResult(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<List<FieldDefinition>>>() {});
        assertSuccess(body);
        assertThat(body.getResult()).extracting(FieldDefinition::getFieldName).containsExactly("note");
    }

    @Test
    @Order(4)
    @DisplayName("Flow 4: Save custom form with items and auto-publish extend field")
    void testFlow4_saveCustomForm() throws Exception {
        String code = formCode();
        CustomFormsForm form = buildCustomFormsForm(code, "OpenAPI Custom Form");

        MvcResult result = mockMvc.perform(post(CUSTOM_FORMS_BASE + "/saveData")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.code").value(code))
                .andReturn();

        ResultBody<CustomForms> body = parseResult(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<CustomForms>>() {});
        assertSuccess(body);
        assertThat(body.getResult().getId()).isNotNull();
        assertThat(redisContainsFormScope("0:" + code)).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("Flow 5: Get custom form detail with items")
    void testFlow5_getCustomFormDetail() throws Exception {
        String code = formCode();
        CustomFormsForm query = new CustomFormsForm();
        query.setCode(code);

        MvcResult result = mockMvc.perform(post(CUSTOM_FORMS_BASE + "/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.code").value(code))
                .andReturn();

        ResultBody<CustomFormsResult> body = parseResult(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<CustomFormsResult>>() {});
        assertSuccess(body);
        assertThat(body.getResult().getCustomFormsItemList()).isNotEmpty();
        assertThat(body.getResult().getCustomFormsItemList().get(0).getFieldName()).isEqualTo("note");
    }

    @Test
    @Order(6)
    @DisplayName("Flow 6: Re-publish extend form definition from DB to Redis")
    void testFlow6_publishExtendForm() throws Exception {
        String code = formCode();
        mockMvc.perform(post(EXTEND_FORM_BASE + "/{formCode}/publish", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result").value(true));

        assertThat(redisContainsFormScope("0:" + code)).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("Flow 7: Update extend form definition and re-publish")
    void testFlow7_updateExtendFormDefinition() throws Exception {
        String code = formCode();
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("OpenAPI Extend Form Updated");
        FieldDefinition note = new FieldDefinition();
        note.setFieldName("note");
        note.setFieldLabel("Note");
        note.setFieldType("string");
        FieldDefinition score = new FieldDefinition();
        score.setFieldName("score");
        score.setFieldLabel("Score");
        score.setFieldType("number");
        request.setFields(Arrays.asList(note, score));

        MvcResult result = mockMvc.perform(put(EXTEND_FORM_BASE + "/{formCode}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ResultBody<ExtendFormDefinition> body = parseResult(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<ExtendFormDefinition>>() {});
        assertSuccess(body);
        assertThat(body.getResult().getFormName()).isEqualTo("OpenAPI Extend Form Updated");
        assertThat(body.getResult().getFields()).hasSize(2);

        MvcResult redisResult = mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}/definitions", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        ResultBody<List<FieldDefinition>> redisBody = parseResult(
                redisResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<List<FieldDefinition>>>() {});
        assertSuccess(redisBody);
        assertThat(redisBody.getResult()).hasSize(2);
    }

    @Test
    @Order(8)
    @DisplayName("Flow 8: End-to-end custom form linked with extend field definition")
    void testFlow8_endToEndCustomFormWorkflow() throws Exception {
        String e2eCode = "it_e2e_" + System.nanoTime();

        SaveExtendFormRequest extendReq = new SaveExtendFormRequest();
        extendReq.setFormName("E2E Extend Form");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("remark");
        field.setFieldLabel("Remark");
        field.setFieldType("string");
        extendReq.setFields(Collections.singletonList(field));

        mockMvc.perform(post(EXTEND_FORM_BASE + "/{formCode}", e2eCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(extendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        CustomFormsForm customForm = buildCustomFormsForm(e2eCode, "E2E Custom Form");
        customForm.getCustomFormsItemList().get(0).setFieldName("remark");
        customForm.getCustomFormsItemList().get(0).setLabelName("Remark");

        MvcResult saveResult = mockMvc.perform(post(CUSTOM_FORMS_BASE + "/saveData")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(customForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ResultBody<CustomForms> saved = parseResult(
                saveResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<CustomForms>>() {});
        assertSuccess(saved);
        Long customFormId = saved.getResult().getId();

        CustomFormsForm detailQuery = new CustomFormsForm();
        detailQuery.setCode(e2eCode);
        MvcResult detailResult = mockMvc.perform(post(CUSTOM_FORMS_BASE + "/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(detailQuery)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ResultBody<CustomFormsResult> detail = parseResult(
                detailResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<CustomFormsResult>>() {});
        assertSuccess(detail);
        assertThat(detail.getResult().getName()).isEqualTo("E2E Custom Form");

        MvcResult dbResult = mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}", e2eCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        ResultBody<ExtendFormDefinition> fromDb = parseResult(
                dbResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<ExtendFormDefinition>>() {});
        assertSuccess(fromDb);
        assertThat(fromDb.getResult().getCustomFormId()).isEqualTo(customFormId);
        assertThat(redisContainsFormScope("0:" + e2eCode)).isTrue();
    }

    private static CustomFormsForm buildCustomFormsForm(String code, String name) {
        CustomFormsForm form = new CustomFormsForm();
        form.setCode(code);
        form.setName(name);
        form.setFormOrTable(FormOrTable.form);
        form.setAutoPublishExtendField(true);

        CustomFormsItem item = new CustomFormsItem();
        item.setFieldName("note");
        item.setLabelName("Note");
        item.setFieldType(FieldType.text);
        item.setComponentType(ComponentType.input);
        form.setCustomFormsItemList(Collections.singletonList(item));
        return form;
    }

    private static <T> ResultBody<T> parseResult(String json, TypeReference<ResultBody<T>> type) {
        return JSON.parseObject(json, type);
    }

    private static void assertSuccess(ResultBody<?> body) {
        assertThat(body).isNotNull();
        assertThat(body.getSuccess())
                .as("API failed: %s", body.getMessage())
                .isTrue();
        assertThat(body.getCode()).isEqualTo(200);
    }
}
'''

if __name__ == "__main__":
    TARGET.write_text(JAVA, encoding="utf-8")
    print("written", TARGET, "bytes", TARGET.stat().st_size)
