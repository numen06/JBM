package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;

import java.util.List;

public interface ExtendFormDefinitionService {

    ExtendFormDefinition saveAndPublish(String formCode, SaveExtendFormRequest request);

    void publishToRedis(String formCode);

    ExtendFormDefinition getByFormCode(String formCode);

    void publishFromCustomForms(Long customFormId, String formCode, String formName, List<FieldDefinition> fields);

    List<FieldDefinition> listFromRedis(String formCode);
}
