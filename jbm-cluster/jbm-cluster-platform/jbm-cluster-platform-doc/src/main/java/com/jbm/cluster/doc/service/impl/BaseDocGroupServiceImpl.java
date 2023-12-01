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


    /**
     * @param baseDocGroup
     * @return
     */
    @Override
    public BaseDocGroup createTempGroup(BaseDocGroup baseDocGroup) {
        if (ObjectUtil.isEmpty(baseDocGroup)) {
//            throw new ServiceException("参数错误");
            baseDocGroup = new BaseDocGroup();
        }
        if (ObjectUtil.isEmpty(baseDocGroup.getGroupPath())) {
            baseDocGroup.setGroupPath(IdUtil.fastSimpleUUID());
        }
        baseDocGroup = this.saveEntity(baseDocGroup);
        BaseDocToken docToken = baseDocTokenService.createGroupToken(baseDocGroup.getExpirationTime(), baseDocGroup.getGroupId());
        baseDocGroup.setTokenKey(docToken.getTokenKey());
        return baseDocGroup;
    }

    @Override
    public BaseDocGroup findGroupById(String groupId) {
        if (ObjectUtil.isEmpty(groupId)) {
            throw new ServiceException("分组ID为空");
        }
        QueryWrapper<BaseDocGroup> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseDocGroup::getGroupId, groupId);
        BaseDocGroup baseDocGroup = this.selectEntityByWapper(queryWrapper);
        return baseDocGroup;
    }

    @Override
    public List<BaseDoc> findGroupItems(String groupId) {
        BaseDocGroup baseDocGroup = this.findGroupById(groupId);
        if (ObjectUtil.isEmpty(baseDocGroup)) {
            throw new ServiceException("没有找到对应的分组");
        }
        return baseDocService.findGroupItemsByPath(baseDocGroup.getGroupPath());
    }

    @Override
    public boolean removeGroupItemsByPath(List<String> paths) {
        return baseDocService.removeByPaths(paths);
    }

    @Override
    public boolean removeGroupItemsById(List<String> ids) {
        return baseDocService.removeByIds(ids);
    }

}