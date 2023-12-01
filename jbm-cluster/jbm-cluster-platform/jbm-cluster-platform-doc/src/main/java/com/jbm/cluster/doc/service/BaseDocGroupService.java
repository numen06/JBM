package com.jbm.cluster.doc.service;

import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.api.entitys.doc.BaseDocGroup;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-11-28 17:20:20
 */
public interface BaseDocGroupService extends IMasterDataService<BaseDocGroup> {


//    BaseDocGroup createTempGroup();

    BaseDocGroup createTempGroup(BaseDocGroup baseDocGroup);



    BaseDocGroup findGroupById(String groupId);

    List<BaseDoc> findGroupItems(String groupId);

    boolean removeGroupItemsByPath(List<String> paths);

    boolean removeGroupItemsById(List<String> paths);
}
