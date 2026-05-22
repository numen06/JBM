package com.jbm.cluster.center.integration;

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
    @DisplayName("Flow 1: Save extend form definition")
    void testFlow1_saveExtendFormDefinition() throws Exception {
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("OpenAPI Extend Form");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("note");
        field.setFieldLabel("Note");
        field.setFieldType("string");
        request.setFields(Collections.singletonList(field));
        String code = formCode();
        mockMvc.perform(post(EXTEND_FORM_BASE + "/{formCode}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(redisContainsFormScope("0:" + code)).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Flow 2: Save custom form")
    void testFlow2_saveCustomForm() throws Exception {
        String code = formCode();
        CustomFormsForm form = new CustomFormsForm();
        form.setCode(code);
        form.setName("OpenAPI Custom Form");
        form.setFormOrTable(FormOrTable.form);
        form.setAutoPublishExtendField(true);
        CustomFormsItem item = new CustomFormsItem();
        item.setFieldName("note");
        item.setLabelName("Note");
        item.setFieldType(FieldType.text);
        item.setComponentType(ComponentType.input);
        form.setCustomFormsItemList(Collections.singletonList(item));
        mockMvc.perform(post(CUSTOM_FORMS_BASE + "/saveData")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.code").value(code));
    }

    @Test
    @Order(3)
    @DisplayName("Flow 3: Get custom form detail")
    void testFlow3_getCustomFormDetail() throws Exception {
        String code = formCode();
        CustomFormsForm query = new CustomFormsForm();
        query.setCode(code);
        mockMvc.perform(post(CUSTOM_FORMS_BASE + "/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.code").value(code));
    }

    @Test
    @Order(4)
    @DisplayName("Flow 4: Get extend form from DB")
    void testFlow4_getExtendFormFromDb() throws Exception {
        String code = formCode();
        mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.formCode").value(code));
    }

    @Test
    @Order(5)
    @DisplayName("Flow 5: List definitions from Redis")
    void testFlow5_listFromRedis() throws Exception {
        String code = formCode();
        MvcResult result = mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}/definitions", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        ResultBody<List<FieldDefinition>> body = JSON.parseObject(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<List<FieldDefinition>>>() {});
        assertThat(body.getSuccess()).isTrue();
        assertThat(body.getResult()).isNotEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("Flow 6: Publish extend form")
    void testFlow6_publish() throws Exception {
        mockMvc.perform(post(EXTEND_FORM_BASE + "/{formCode}/publish", formCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(7)
    @DisplayName("Flow 7: Update extend form")
    void testFlow7_updateExtendForm() throws Exception {
        String code = formCode();
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("OpenAPI Extend Form Updated");
        FieldDefinition f1 = new FieldDefinition();
        f1.setFieldName("note");
        f1.setFieldType("string");
        FieldDefinition f2 = new FieldDefinition();
        f2.setFieldName("score");
        f2.setFieldType("number");
        request.setFields(Arrays.asList(f1, f2));
        mockMvc.perform(put(EXTEND_FORM_BASE + "/{formCode}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(8)
    @DisplayName("Flow 8: End-to-end workflow")
    void testFlow8_endToEnd() throws Exception {
        String e2eCode = "it_e2e_" + System.nanoTime();
        SaveExtendFormRequest extendReq = new SaveExtendFormRequest();
        extendReq.setFormName("E2E Extend Form");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("remark");
        field.setFieldType("string");
        extendReq.setFields(Collections.singletonList(field));
        mockMvc.perform(post(EXTEND_FORM_BASE + "/{formCode}", e2eCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(extendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        CustomFormsForm form = new CustomFormsForm();
        form.setCode(e2eCode);
        form.setName("E2E Custom Form");
        form.setFormOrTable(FormOrTable.form);
        form.setAutoPublishExtendField(true);
        CustomFormsItem item = new CustomFormsItem();
        item.setFieldName("remark");
        item.setLabelName("Remark");
        item.setFieldType(FieldType.text);
        item.setComponentType(ComponentType.input);
        form.setCustomFormsItemList(Collections.singletonList(item));

        MvcResult save = mockMvc.perform(post(CUSTOM_FORMS_BASE + "/saveData")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        ResultBody<CustomForms> saved = JSON.parseObject(
                save.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<CustomForms>>() {});
        assertThat(saved.getSuccess()).isTrue();

        CustomFormsForm q = new CustomFormsForm();
        q.setCode(e2eCode);
        mockMvc.perform(post(CUSTOM_FORMS_BASE + "/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(q)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult db = mockMvc.perform(get(EXTEND_FORM_BASE + "/{formCode}", e2eCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        ResultBody<ExtendFormDefinition> fromDb = JSON.parseObject(
                db.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<ResultBody<ExtendFormDefinition>>() {});
        assertThat(fromDb.getSuccess()).isTrue();
        assertThat(fromDb.getResult().getCustomFormId()).isEqualTo(saved.getResult().getId());
        assertThat(redisContainsFormScope("0:" + e2eCode)).isTrue();
    }
}