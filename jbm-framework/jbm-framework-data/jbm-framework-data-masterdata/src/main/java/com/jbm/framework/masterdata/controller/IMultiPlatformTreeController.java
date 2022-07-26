package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.usage.entity.MultiPlatformTreeEntity;
import com.jbm.framework.metadata.bean.ResultBody;

import java.util.List;

public interface IMultiPlatformTreeController<Entity extends MultiPlatformTreeEntity> extends IMultiPlatformController<Entity> {

    /**
     * @return
     */
    ResultBody<List<Entity>> root(Entity entity);

    /**
     * @return
     */
    ResultBody<List<Entity>> tree(Entity entity);
}
