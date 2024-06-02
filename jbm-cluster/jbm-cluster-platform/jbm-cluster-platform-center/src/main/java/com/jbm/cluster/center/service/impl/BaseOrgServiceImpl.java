package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.center.service.BaseOrgService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MultiPlatformTreeServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
@Service
public class BaseOrgServiceImpl extends MultiPlatformTreeServiceImpl<BaseOrg> implements BaseOrgService {

    @Override
    public List<BaseOrg> selectEntitys(BaseOrg org) {
        if (ObjectUtil.isNotEmpty(org)) {
            return super.selectEntitys(org);
        }
        // 获取当前公司的顶层公司
        BaseOrg baseOrg = this.selectById(LoginHelper.getDeptId());
        BaseOrg parentOrg = this.findTopCompany(baseOrg);
        // 获取顶层公司下的所有公司
        return this.findRelegationCompany(parentOrg);
    }

    @Override
    public BaseOrg findTopCompany(BaseOrg org) {
        BaseOrg baseOrg;
        if (ObjectUtil.isNotEmpty(org.getParentId())) {
            // 查询上级公司
            baseOrg = this.selectById(org.getParentId());
        } else {
            Assert.notNull(org.getId(), () -> new ServiceException("没有部门ID"));
            baseOrg = this.selectById(org.getId());
            Assert.notNull(baseOrg, () -> new ServiceException("未查询到对应部门"));
        }
        while (true) {
            // 已经是顶层节点直接返回
            if (ObjectUtil.isEmpty(baseOrg.getParentId())) {
                return baseOrg;
            }
            baseOrg = this.selectById(baseOrg.getParentId());
        }
    }

    @Override
    public List<BaseOrg> findRelegationCompany(BaseOrg org) {
        Assert.notNull(org.getId(), () -> new ServiceException("没有部门ID"));
        BaseOrg baseOrg = this.selectById(org.getId());
        Assert.notNull(baseOrg, () -> new ServiceException("未查询到对应部门"));
        return findRelegationCompany(baseOrg, ListUtil.toList(baseOrg));
    }

    /***
     * 获取下级公司
     * @param org 当前组织
     * @param baseOrgs 组织合集
     * @return
     */
    private List<BaseOrg> findRelegationCompany(BaseOrg org, List<BaseOrg> baseOrgs) {
        BaseOrg orgPram = new BaseOrg();
        orgPram.setParentId(org.getId());
        List<BaseOrg> subOrgs = this.selectEntitys(orgPram);
        for (BaseOrg subOrg : subOrgs) {
            baseOrgs.add(subOrg);
            this.findRelegationCompany(subOrg, baseOrgs);
        }
        return baseOrgs;
    }
}
