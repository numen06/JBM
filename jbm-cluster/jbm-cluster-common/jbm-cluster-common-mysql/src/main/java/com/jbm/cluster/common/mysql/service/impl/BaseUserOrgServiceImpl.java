package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.entitys.basic.BaseUserOrg;
import com.jbm.cluster.common.mysql.mapper.BaseUserOrgMapper;
import com.jbm.cluster.common.mysql.service.BaseUserOrgService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseUserOrgServiceImpl extends MasterDataServiceImpl<BaseUserOrg> implements BaseUserOrgService {

    @Autowired
    private BaseUserOrgMapper baseUserOrgMapper;
    @Autowired
    private BaseUserService baseUserService;

    @Override
    public List<BaseUserOrg> findUserOrgs(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        QueryWrapper<BaseUserOrg> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseUserOrg::getUserId, userId);
        return baseUserOrgMapper.selectList(queryWrapper);
    }

    @Override
    public List<Long> getActiveOrgIds(Long userId) {
        Date now = new Date();
        return findUserOrgs(userId).stream()
                .filter(row -> row.getOrgId() != null)
                .filter(row -> row.getExpireTime() == null || row.getExpireTime().after(now))
                .map(BaseUserOrg::getOrgId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void saveUserOrgs(Long userId, String... orgIds) {
        if (userId == null) {
            return;
        }
        BaseUser user = baseUserService.getUserById(userId);
        if (user == null) {
            return;
        }
        if (JbmConstants.ROOT.equals(user.getUserName())) {
            throw new ServiceException("默认用户无需分配组织授权");
        }
        removeUserOrgs(userId);
        if (orgIds == null || orgIds.length == 0) {
            return;
        }
        for (String orgId : orgIds) {
            if (ObjectUtil.isEmpty(orgId)) {
                continue;
            }
            Long parsed = Long.parseLong(orgId.trim());
            if (ObjectUtil.equal(parsed, user.getCompanyId())) {
                continue;
            }
            BaseUserOrg row = new BaseUserOrg();
            row.setUserId(userId);
            row.setOrgId(parsed);
            baseUserOrgMapper.insert(row);
        }
    }

    @Override
    public void removeUserOrgs(Long userId) {
        if (userId == null) {
            return;
        }
        QueryWrapper<BaseUserOrg> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseUserOrg::getUserId, userId);
        baseUserOrgMapper.delete(queryWrapper);
    }
}
