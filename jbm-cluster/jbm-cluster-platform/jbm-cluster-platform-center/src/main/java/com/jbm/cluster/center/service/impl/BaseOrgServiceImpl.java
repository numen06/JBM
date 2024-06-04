package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.center.service.BaseOrgService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MultiPlatformTreeServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
@Service
public class BaseOrgServiceImpl extends MultiPlatformTreeServiceImpl<BaseOrg> implements BaseOrgService {

    @Override
    public List<BaseOrg> selectEntitys(BaseOrg baseOrg) {
        // 超级管理员账号查询所有数据
        if (LoginHelper.isAdmin()) {
            return super.selectEntitys(baseOrg);
        }
        BaseOrg currentOrg = this.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            return null;
        }
        // 获取当前公司的顶层公司
        BaseOrg parentOrg = this.findTopCompany(currentOrg);
        // 避免查询条件为空的情况
        baseOrg = ObjectUtil.isEmpty(baseOrg) ? new BaseOrg() : baseOrg;
        // 根据顶层公司进行过滤
        baseOrg.setGroupId(parentOrg.getId().toString());
        return super.selectEntitys(baseOrg);
    }

    @Override
    public DataPaging<BaseOrg> selectEntitys(BaseOrg baseOrg, PageForm pageForm) {
        // 超级管理员账号查询所有数据
        if (LoginHelper.isAdmin()) {
            return super.selectEntitys(baseOrg, pageForm);
        }
        BaseOrg currentOrg = this.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            return null;
        }
        // 获取当前公司的顶层公司
        BaseOrg parentOrg = this.findTopCompany(currentOrg);
        // 避免查询条件为空的情况
        baseOrg = ObjectUtil.isEmpty(baseOrg) ? new BaseOrg() : baseOrg;
        // 根据顶层公司进行过滤
        baseOrg.setGroupId(parentOrg.getId().toString());
        return super.selectEntitys(baseOrg, pageForm);
    }

    @Override
    public BaseOrg saveEntity(BaseOrg baseOrg) {
        BaseOrg rootOrg = this.findTopCompany(baseOrg);
        baseOrg.setGroupId(rootOrg.getId().toString());
        // 账户数量为空的情况下写入默认一个账户
        if (ObjectUtil.isEmpty(baseOrg.getNumberOfAccounts())) {
            baseOrg.setNumberOfAccounts(1);
        }
        return super.saveEntity(baseOrg);
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
        List<BaseOrg> subOrgs = super.selectEntitys(orgPram);
        for (BaseOrg subOrg : subOrgs) {
            baseOrgs.add(subOrg);
            this.findRelegationCompany(subOrg, baseOrgs);
        }
        return baseOrgs;
    }
}
