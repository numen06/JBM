package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;

import java.util.List;

public interface ExtendFormDefinitionService {

    DataPaging<ExtendFormDefinition> pageByTenant(String keyword, PageForm pageForm);

    ExtendFormDefinition saveAndPublish(String formCode, SaveExtendFormRequest request);

    void publishToRedis(String formCode);

    ExtendFormDefinition getByFormCode(String formCode);

    void publishFromCustomForms(Long customFormId, String formCode, String formName, List<FieldDefinition> fields);

    List<FieldDefinition> listFromRedis(String formCode);
}
