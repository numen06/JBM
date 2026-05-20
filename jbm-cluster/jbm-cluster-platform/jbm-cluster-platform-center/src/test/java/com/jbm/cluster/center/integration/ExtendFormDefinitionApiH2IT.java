package com.jbm.cluster.center.integration;

import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.cluster.center.controller.ExtendFormDefinitionController;
import com.jbm.cluster.center.integration.support.ExtendFieldH2RedisTestSupport;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Center 扩展字段定义：保存入库并发布到 Redis（H2 + Mock Redis）。
 */
class ExtendFormDefinitionApiH2IT extends ExtendFieldH2RedisTestSupport {

    @Autowired
    private ExtendFormDefinitionController extendFormDefinitionController;

    @Test
    @DisplayName("保存并发布：库表 + Redis 作用域 0:formCode")
    void saveAndPublish_persistsDbAndRedis() {
        String formCode = "it_extend_form_" + System.nanoTime();
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("集成测试表单");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("note");
        field.setFieldLabel("备注");
        field.setFieldType("string");
        request.setFields(Collections.singletonList(field));

        ResultBody<ExtendFormDefinition> saved =
                extendFormDefinitionController.save(formCode, request);
        assertSuccess(saved);
        assertThat(saved.getResult().getFormCode()).isEqualTo(formCode);
        assertThat(saved.getResult().getFields()).hasSize(1);

        assertThat(redisContainsFormScope("0:" + formCode)).isTrue();

        ResultBody<List<FieldDefinition>> fromRedis =
                extendFormDefinitionController.listFromRedis(formCode);
        assertSuccess(fromRedis);
        assertThat(fromRedis.getResult()).extracting(FieldDefinition::getFieldName)
                .containsExactly("note");
    }

    @Test
    @DisplayName("publish 接口：从库刷新 Redis")
    void publish_refreshesRedisFromDb() {
        String formCode = "it_publish_" + System.nanoTime();
        SaveExtendFormRequest request = new SaveExtendFormRequest();
        request.setFormName("发布测试");
        FieldDefinition field = new FieldDefinition();
        field.setFieldName("score");
        field.setFieldType("number");
        request.setFields(Collections.singletonList(field));

        assertSuccess(extendFormDefinitionController.save(formCode, request));
        assertSuccess(extendFormDefinitionController.publish(formCode));

        ResultBody<ExtendFormDefinition> fromDb =
                extendFormDefinitionController.getFromDb(formCode);
        assertSuccess(fromDb);
        assertThat(fromDb.getResult().getFormName()).isEqualTo("发布测试");
    }

    private static void assertSuccess(ResultBody<?> body) {
        assertThat(body).isNotNull();
        assertThat(body.getSuccess())
                .as("接口失败: %s", body.getMessage())
                .isTrue();
        assertThat(body.getCode()).isEqualTo(200);
    }
}
