package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;

import java.util.List;
import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
public interface BaseDicService extends IMasterDataTreeService<BaseDic> {
    Map<String, List<BaseDic>> getDicMap();

    BaseDic getBaseDicType(String code);

    BaseDic getBaseDic(Long parentId, String code);

    /** 分页查询字典分组（根节点） */
    DataPaging<BaseDic> pageRootList(String keyword, PageForm pageForm);

    /** 分页查询指定分组下的字典项 */
    DataPaging<BaseDic> pageItemsByParentId(Long parentId, String keyword, PageForm pageForm);
}
