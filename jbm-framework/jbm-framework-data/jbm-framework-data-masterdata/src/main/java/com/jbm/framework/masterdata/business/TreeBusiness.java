package com.jbm.framework.masterdata.business;

import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 树形主数据业务基类，封装常用树查询（事务方法请在子类或编排入口上声明）。
 */
public abstract class TreeBusiness<E extends MasterDataTreeEntity, S extends IMasterDataTreeService<E>> extends BaseBusiness {

    @Autowired
    protected S treeService;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<E> listRoots() {
        return treeService.selectRootListById();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<E> listTree(E rootOrFilter) {
        return treeService.selectChildNodesById(rootOrFilter);
    }
}
