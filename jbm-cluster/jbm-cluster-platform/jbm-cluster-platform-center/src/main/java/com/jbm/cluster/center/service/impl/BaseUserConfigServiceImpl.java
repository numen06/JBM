package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseUserConfig;
import com.jbm.cluster.center.service.BaseUserConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 11:19:05
 */
@Slf4j
@Service
public class BaseUserConfigServiceImpl extends MasterDataServiceImpl<BaseUserConfig> implements BaseUserConfigService {

    @Override
    public BaseUserConfig findByUserIdAndAppId(Long userId, Long appId) {
        QueryWrapper<BaseUserConfig> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(BaseUserConfig::getUserId, userId)
                .eq(BaseUserConfig::getAppId, appId)
                .orderByDesc(BaseUserConfig::getUpdateTime)
                .orderByDesc(BaseUserConfig::getId);
        List<BaseUserConfig> configs = this.selectEntitysByWapper(wrapper);
        if (CollUtil.size(configs) > 1) {
            log.warn("用户配置存在重复记录 userId={}, appId={}, count={}，返回最近更新的一条", userId, appId, configs.size());
        }
        return CollUtil.getFirst(configs);
    }

    @Override
    public BaseUserConfig saveEntity(BaseUserConfig entity) {
        Long currentUserId = LoginHelper.getUserId();
        entity.setAppId(LoginHelper.getLoginUser().getAppId());
        if (ObjectUtil.isEmpty(entity.getUserId()) || ObjectUtil.equal(entity.getUserId(), currentUserId)) {
            entity.setUserId(currentUserId);
        } else if (!LoginHelper.isAdmin()) {
            throw new ServiceException("无权保存其他用户配置");
        }
        if (ObjectUtil.isEmpty(entity.getId())) {
            BaseUserConfig baseUserConfig = this.findByUserIdAndAppId(entity.getUserId(), entity.getAppId());
            if (ObjectUtil.isNotEmpty(baseUserConfig)) {
                entity.setId(baseUserConfig.getId());
            }
        }
        return super.saveEntity(entity);
    }

}