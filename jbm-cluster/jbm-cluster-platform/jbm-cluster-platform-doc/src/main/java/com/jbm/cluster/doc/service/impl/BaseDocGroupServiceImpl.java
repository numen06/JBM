package com.jbm.cluster.doc.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.api.entitys.doc.BaseDocGroup;
import com.jbm.cluster.api.entitys.doc.BaseDocToken;
import com.jbm.cluster.doc.service.BaseDocGroupService;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.cluster.doc.service.BaseDocTokenService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-11-28 17:20:20
 */
@Service
public class BaseDocGroupServiceImpl extends MasterDataServiceImpl<BaseDocGroup> implements BaseDocGroupService {


    @Autowired
    private BaseDocTokenService baseDocTokenService;

    @Autowired
    private BaseDocService baseDocService;

    @Override
    public BaseDocGroup createTempGroup() {
        BaseDocToken docToken = baseDocTokenService.createDayToken();
        BaseDocGroup baseDocGroup = new BaseDocGroup();
        baseDocGroup.setExpirationTime(docToken.getExpirationTime());
        baseDocGroup.setTokenKey(docToken.getTokenKey());
        return this.saveEntity(baseDocGroup);
    }

    /**
     * @param baseDocGroup
     * @return
     */
    @Override
    public BaseDocGroup createTempGroup(BaseDocGroup baseDocGroup) {
        if (ObjectUtil.isEmpty(baseDocGroup)) {
            throw new ServiceException("参数错误");
        }
        if (ObjectUtil.isEmpty(baseDocGroup.getGroupPath())) {
            baseDocGroup.setGroupPath(IdUtil.fastSimpleUUID());
        }
        BaseDocToken docToken = baseDocTokenService.createToken(baseDocGroup.getExpirationTime());
        baseDocGroup.setTokenKey(docToken.getTokenKey());
        return this.saveEntity(baseDocGroup);
    }

//    @Override
//    public Boolean checkGroup(String groupPath) {
//        QueryWrapper<BaseDocGroup> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(BaseDocGroup::getGroupPath, groupPath);
//        BaseDocGroup baseDocGroup = this.selectEntityByWapper(queryWrapper);
//        return ObjectUtil.isNotNull(baseDocGroup);
//    }

    @Override
    public List<BaseDoc> findGroupItemsByPath(BaseDocGroup baseDocGroup ) {

        return baseDocService.findGroupItemsByPath(baseDocGroup.getGroupPath());
    }
}