package com.jbm.cluster.center.integration;

import com.jbm.cluster.api.constants.center.ComponentType;
import com.jbm.cluster.api.constants.center.FieldType;
import com.jbm.cluster.api.constants.center.FormOrTable;
import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.center.integration.support.ExtendFieldH2RedisTestSupport;
import com.jbm.cluster.common.mysql.service.CustomFormsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomForms saveData 后按 code 自动发布扩展字段定义。
 */
class CustomFormsExtendFieldH2IT extends ExtendFieldH2RedisTestSupport {

    @Autowired
    private CustomFormsService customFormsService;

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
    }
}
