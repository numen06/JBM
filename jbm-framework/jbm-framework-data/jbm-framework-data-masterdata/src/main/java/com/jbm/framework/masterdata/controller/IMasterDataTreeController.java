package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.swagger.annotation.ApiJsonObject;
import org.springframework.cache.annotation.EnableCaching;

import java.util.List;

/**
 * 带有树形结构的数据库访问类
 *
 * @param <Entity>
 * @author wesley
 */
public interface IMasterDataTreeController<Entity extends MasterDataTreeEntity, Service extends IMasterDataTreeService<Entity>> extends IMasterDataController<Entity, Service> {
    /**
     * @param masterDataRequsetBody
     * @return
     */
    ResultBody<List<Entity>> root(MasterDataRequsetBody masterDataRequsetBody);

    /**
     * @param masterDataRequsetBody
     * @return
     */
    ResultBody<List<Entity>> tree(MasterDataRequsetBody masterDataRequsetBody);

}
