package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.common.mysql.service.BaseOrgService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataTreeServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
@Service
public class BaseOrgServiceImpl extends MasterDataTreeServiceImpl<BaseOrg> implements BaseOrgService {

    @Override
    public List<BaseOrg> selectEntitys(BaseOrg baseOrg) {
        // 超级管理员账号查询所有数据
        if (LoginHelper.isAdmin()) {
            return super.selectEntitys(baseOrg);
        }
        if (ObjectUtil.isEmpty(LoginHelper.softGetLoginUser())) {
            throw new ServiceException("用户没有登录");
        }
        Long companyId = LoginHelper.getCompanyId();
        if (ObjectUtil.isNotEmpty(companyId)) {
            baseOrg = ObjectUtil.isEmpty(baseOrg) ? new BaseOrg() : baseOrg;
            baseOrg.setGroupId(companyId.toString());
            return super.selectEntitys(baseOrg);
        }
        BaseOrg currentOrg = this.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            throw new ServiceException("未查询到对应部门");
        }
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
        if (ObjectUtil.isEmpty(LoginHelper.softGetLoginUser()) || LoginHelper.isAdmin()) {
            return super.selectEntitys(baseOrg, pageForm);
        }
        Long companyId = LoginHelper.getCompanyId();
        if (ObjectUtil.isNotEmpty(companyId)) {
            baseOrg = ObjectUtil.isEmpty(baseOrg) ? new BaseOrg() : baseOrg;
            baseOrg.setGroupId(companyId.toString());
            return super.selectEntitys(baseOrg, pageForm);
        }
        BaseOrg currentOrg = this.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            return null;
        }
        BaseOrg parentOrg = this.findTopCompany(currentOrg);
        // 避免查询条件为空的情况
        baseOrg = ObjectUtil.isEmpty(baseOrg) ? new BaseOrg() : baseOrg;
        // 根据顶层公司进行过滤
        baseOrg.setGroupId(parentOrg.getId().toString());
        return super.selectEntitys(baseOrg, pageForm);
    }

    @Override
    public BaseOrg saveEntity(BaseOrg baseOrg) {
        baseOrg = super.saveEntity(baseOrg);
        if (ObjectUtil.isNotEmpty(selectById(baseOrg.getParentId()))) {
            BaseOrg rootOrg = this.findTopCompany(baseOrg);
            baseOrg.setGroupId(rootOrg.getId().toString());
        } else {
            baseOrg.setGroupId(baseOrg.getId().toString());
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

    @Override
    public List<BaseOrg> selectOrgTree(BaseOrg filter) {
        BaseOrg query = ObjectUtil.isEmpty(filter) ? new BaseOrg() : filter;
        List<BaseOrg> flat = this.selectChildNodesById(query.getId());
        return buildOrgTree(flat);
    }

    private List<BaseOrg> buildOrgTree(List<BaseOrg> flat) {
        if (CollUtil.isEmpty(flat)) {
            return new ArrayList<>();
        }
        Map<Long, BaseOrg> byId = new LinkedHashMap<>();
        for (BaseOrg org : flat) {
            if (org.getId() == null) {
                continue;
            }
            org.setChildren(new ArrayList<>());
            byId.put(org.getId(), org);
        }
        List<BaseOrg> roots = new ArrayList<>();
        for (BaseOrg org : byId.values()) {
            Long parentId = org.getParentId();
            BaseOrg parent = parentId != null ? byId.get(parentId) : null;
            if (parent != null) {
                parent.getChildren().add(org);
            } else {
                roots.add(org);
            }
        }
        sortOrgTree(roots);
        return roots;
    }

    private void sortOrgTree(List<BaseOrg> nodes) {
        nodes.sort(Comparator
                .comparing((BaseOrg o) -> ObjectUtil.defaultIfNull(o.getId(), 0L))
                .thenComparing(BaseOrg::getOrgName, Comparator.nullsLast(String::compareTo)));
        for (BaseOrg node : nodes) {
            if (CollUtil.isNotEmpty(node.getChildren())) {
                sortOrgTree(node.getChildren());
            }
        }
    }

    @Override
    public BaseOrg getBaseOrg(BaseOrg baseOrg) {
        List<BaseOrg> baseOrgList = this.lambdaQuery()
                .eq(ObjectUtil.isNotNull(baseOrg.getOrgCode()), BaseOrg::getOrgCode, baseOrg.getOrgCode())
                .eq(ObjectUtil.isNotNull(baseOrg.getId()), BaseOrg::getId, baseOrg.getId())
                .eq(ObjectUtil.isNotNull(baseOrg.getOrgName()), BaseOrg::getOrgName, baseOrg.getOrgName())
                .list();
        //默认返回第一条、会存在参数未传的情况
        return CollUtil.getFirst(baseOrgList);
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
