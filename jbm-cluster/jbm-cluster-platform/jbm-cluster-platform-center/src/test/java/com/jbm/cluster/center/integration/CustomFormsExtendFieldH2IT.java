package com.jbm.cluster.center.integration;

import com.jbm.cluster.api.constants.center.ComponentType;
import com.jbm.cluster.api.constants.center.FieldType;
import com.jbm.cluster.api.constants.center.FormOrTable;
import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.api.result.CustomFormsResult;
import com.jbm.cluster.center.integration.support.ExtendFieldH2RedisTestSupport;
import com.jbm.cluster.common.mysql.service.CustomFormsService;
import com.jbm.cluster.common.mysql.service.ExtendFormDefinitionService;
import com.jbm.framework.exceptions.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CustomForms saveData 后按 code 自动发布扩展字段定义。
 */
class CustomFormsExtendFieldH2IT extends ExtendFieldH2RedisTestSupport {

    @Autowired
    private CustomFormsService customFormsService;

    @Autowired
    private ExtendFormDefinitionService extendFormDefinitionService;

    @Test
    @DisplayName("saveData 带 code 时自动 publish 到 Redis")
    void saveData_withCode_autoPublishesExtendField() {
        String formCode = "it_custom_" + System.nanoTime();
        CustomFormsForm form = new CustomFormsForm();
        form.setCode(formCode);
        form.setName("自定义表单集成测试");
        form.setFormOrTable(FormOrTable.form);
        form.setAutoPublishExtendField(true);

        CustomFormsItem item = new CustomFormsItem();
        item.setFieldName("note");
        item.setLabelName("备注");
        item.setFieldType(FieldType.text);
        item.setComponentType(ComponentType.input);
        form.setCustomFormsItemList(Collections.singletonList(item));

        CustomForms saved = customFormsService.saveData(form);
        assertThat(saved).isNotNull();
        assertThat(saved.getCode()).isEqualTo(formCode);

        assertThat(redisContainsFormScope("0:" + formCode)).isTrue();
        ExtendFormDefinition definition = extendFormDefinitionService.getByFormCode(formCode);
        assertThat(definition.getCustomFormId()).isEqualTo(saved.getId());
        assertThat(definition.getFormCode()).isEqualTo(formCode);
    }

    @Test
    @DisplayName("getDetail 支持按 id 与 code 查询")
    void getDetail_supportsIdAndCode() {
        String formCode = "it_detail_" + System.nanoTime();
        CustomForms saved = customFormsService.saveData(buildForm(formCode, "note", true));

        CustomFormsForm byId = new CustomFormsForm();
        byId.setId(saved.getId());
        CustomFormsResult detailById = customFormsService.getDetail(byId);
        assertThat(detailById.getCode()).isEqualTo(formCode);
        assertThat(detailById.getCustomFormsItemList()).hasSize(1);

        CustomFormsForm byCode = new CustomFormsForm();
        byCode.setCode(formCode);
        CustomFormsResult detailByCode = customFormsService.getDetail(byCode);
        assertThat(detailByCode.getId()).isEqualTo(saved.getId());
        assertThat(detailByCode.getCustomFormsItemList()).extracting(CustomFormsItem::getFieldName)
                .containsExactly("note");
    }

    @Test
    @DisplayName("saveData stores and preserves designer detail JSON")
    void saveData_storesAndPreservesDetailJson() {
        String formCode = "it_design_" + System.nanoTime();
        String detailJson = "{\"formItems\":[],\"formConfig\":{\"labelWidth\":120}}";
        CustomFormsForm first = buildForm(formCode, "note", true);
        first.setDetail(detailJson);

        CustomForms saved = customFormsService.saveData(first);
        CustomFormsForm query = new CustomFormsForm();
        query.setId(saved.getId());
        assertThat(customFormsService.getDetail(query).getDetail()).isEqualTo(detailJson);

        customFormsService.saveData(buildForm(formCode, "score", true));
        CustomFormsResult detail = customFormsService.getDetail(query);
        assertThat(detail.getDetail()).isEqualTo(detailJson);
        assertThat(detail.getCustomFormsItemList()).extracting(CustomFormsItem::getFieldName)
                .containsExactly("score");
    }

    @Test
    @DisplayName("重复保存同一 code 时替换字段明细")
    void saveData_sameCode_replacesItems() {
        String formCode = "it_replace_" + System.nanoTime();
        CustomForms first = customFormsService.saveData(buildForm(formCode, "note", true));
        CustomForms second = customFormsService.saveData(buildForm(formCode, "score", true));

        assertThat(second.getId()).isEqualTo(first.getId());
        CustomFormsForm query = new CustomFormsForm();
        query.setCode(formCode);
        CustomFormsResult detail = customFormsService.getDetail(query);
        assertThat(detail.getCustomFormsItemList()).hasSize(1);
        assertThat(detail.getCustomFormsItemList()).extracting(CustomFormsItem::getFieldName)
                .containsExactly("score");
    }

    @Test
    @DisplayName("自动发布缺少 code 时返回明确错误")
    void saveData_autoPublishWithoutCode_throws() {
        CustomFormsForm form = buildForm(null, "note", true);

        assertThatThrownBy(() -> customFormsService.saveData(form))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("code/formCode");
    }

    @Test
    @DisplayName("关闭自动发布时允许只保存设计态")
    void saveData_withoutCodeAndAutoPublishFalse_savesDesignOnly() {
        CustomFormsForm form = buildForm(null, "note", false);

        CustomForms saved = customFormsService.saveData(form);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).isNull();
    }

    private static CustomFormsForm buildForm(String formCode, String fieldName, boolean autoPublish) {
        CustomFormsForm form = new CustomFormsForm();
        form.setCode(formCode);
        form.setName("自定义表单集成测试");
        form.setFormOrTable(FormOrTable.form);
        form.setAutoPublishExtendField(autoPublish);

        CustomFormsItem item = new CustomFormsItem();
        item.setFieldName(fieldName);
        item.setLabelName(fieldName);
        item.setFieldType(FieldType.text);
        item.setComponentType(ComponentType.input);
        item.setIsRequired(true);
        item.setIsFilter(true);
        item.setIsShow(true);
        form.setCustomFormsItemList(Collections.singletonList(item));
        return form;
    }
}
