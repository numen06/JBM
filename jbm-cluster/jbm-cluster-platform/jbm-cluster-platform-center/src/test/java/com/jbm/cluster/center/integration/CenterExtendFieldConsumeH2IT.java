package com.jbm.cluster.center.integration;

import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.cluster.center.controller.ExtendFormDefinitionController;
import com.jbm.cluster.center.integration.support.ExtendFieldH2RedisTestSupport;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Center 运行时消费：发布定义后通过 FieldDefinitionService 读取（同进程 REDIS 源）。
 */
class CenterExtendFieldConsumeH2IT extends ExtendFieldH2RedisTestSupport {

    @Autowired
    private ExtendFormDefinitionController extendFormDefinitionController;

    @Autowired
    private FieldDefinitionService fieldDefinitionService;

    @Test
    @DisplayName("发布后 FieldDefinitionService 可读扩展字段名")
    void afterPublish_fieldDefinitionServiceSeesNames() {
        String formCode = "it_consume_" + System.nanoTime();
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("消费测试");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("region");
        field.setFieldLabel("区域");
        field.setFieldType("string");
        request.setFields(Collections.singletonList(field));

        ResultBody<?> saved = extendFormDefinitionController.save(formCode, request);
        assertThat(saved.getSuccess()).isTrue();

        Set<String> names = fieldDefinitionService.getExtendFieldNames(formCode);
        assertThat(names).containsExactly("region");
    }
}
