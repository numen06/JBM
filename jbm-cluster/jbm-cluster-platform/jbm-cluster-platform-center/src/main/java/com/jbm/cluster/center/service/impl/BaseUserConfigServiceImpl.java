package com.jbm.cluster.center.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseUserConfig;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.center.service.BaseUserConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 11:19:05
 */
@Slf4j
@Service
public class BaseUserConfigServiceImpl extends MasterDataServiceImpl<BaseUserConfig> implements BaseUserConfigService {


    @Override
    public BaseUserConfig saveEntity(BaseUserConfig entity) {
//        LoginHelper.getLoginUser().getAppId();
        entity.setAppId(LoginHelper.getLoginUser().getAppId());
        if (ObjectUtil.isEmpty(entity.getId())) {
//            entity.setAppId(LoginHelper.getLoginUser().getAppId());
            QueryWrapper<BaseUserConfig> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(BaseUserConfig::getUserId, entity.getUserId()).eq(BaseUserConfig::getAppId, entity.getAppId());
            BaseUserConfig baseUserConfig = this.selectEntityByWapper(wrapper);
            if (ObjectUtil.isNotEmpty(baseUserConfig)) {
                entity.setId(baseUserConfig.getId());
            }
        }
//        log.info("保存用户配置");
        return super.saveEntity(entity);
    }

}