package com.jbm.cluster.center.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.constants.OrgType;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.center.service.BaseOrgService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MultiPlatformTreeServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
@Service
public class BaseOrgServiceImpl extends MultiPlatformTreeServiceImpl<BaseOrg> implements BaseOrgService {

    /***
     * 找到顶层公司
     * @return
     */
    @Override
    public BaseOrg findTopCompany(BaseOrg org) {
        BaseOrg porg = null;
        //查询出上级节点
        if (ObjectUtil.isNotEmpty(org.getParentId())) {
            porg = this.selectById(org.getParentId());
        } else {
            if (ObjectUtil.isEmpty(org.getId())) {
                throw new ServiceException("没有部门ID");
            }
            BaseOrg dborg = this.selectById(org.getId());
            if (ObjectUtil.isEmpty(dborg)) {
                throw new ServiceException("上机机构失效");
            }
            //已经是顶层节点直接返回
            if (ObjectUtil.isEmpty(dborg.getParentId())) {
                return dborg;
            }
        }
        while (true) {
            //已经是顶层节点直接返回
            if (ObjectUtil.isEmpty(porg.getParentId())) {
                return porg;
            }
            porg = this.selectById(porg.getParentId());
        }
    }

    /***
     * 找到上级公司
     * @return
     */
    @Override
    public BaseOrg findRelegationCompany(BaseOrg org) {
        BaseOrg porg = null;
        //查询出上级节点
        if (ObjectUtil.isNotEmpty(org.getParentId())) {
            porg = this.selectById(org.getParentId());
        } else {
            if (ObjectUtil.isEmpty(org.getId())) {
                throw new ServiceException("没有部门ID");
            }
            BaseOrg dborg = this.selectById(org.getId());
            if (ObjectUtil.isEmpty(dborg)) {
                throw new ServiceException("上机机构失效");
            }
            //已经是顶层节点直接返回
            if (ObjectUtil.isEmpty(dborg.getParentId())) {
                return dborg;
            }
        }
        while (true) {
            //已经是顶层节点直接返回
            if (ObjectUtil.isEmpty(porg.getParentId())) {
                return porg;
            }
            //如果是公司则返回
            if (OrgType.company.toString().equals(porg.getOrgType())) {
                return porg;
            }
            porg = this.selectById(porg.getParentId());
        }
    }


}
