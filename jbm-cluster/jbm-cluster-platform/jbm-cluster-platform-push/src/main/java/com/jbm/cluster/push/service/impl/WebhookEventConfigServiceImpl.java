package com.jbm.cluster.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.framework.service.mybatis.MultiPlatformServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Service
public class WebhookEventConfigServiceImpl extends MultiPlatformServiceImpl<WebhookEventConfig> implements WebhookEventConfigService {


    @Override
    public WebhookEventConfig selectByEventId(String eventId) {
        QueryWrapper<WebhookEventConfig> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().eq(WebhookEventConfig::getEventId, eventId);
        return this.selectEntityByWapper(queryWrapper);
    }

    @Override
    public boolean deleteOldBatch(String serviceName, String batchTime) {
        QueryWrapper<WebhookEventConfig> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().eq(WebhookEventConfig::getServiceName, serviceName).notIn(WebhookEventConfig::getBatchTime, batchTime);
        return this.deleteByWapper(queryWrapper);
    }

    @Override
    public boolean disableEvents(String serviceName) {
        UpdateWrapper<WebhookEventConfig> updateWrapper = new UpdateWrapper();
        updateWrapper.lambda().eq(WebhookEventConfig::getServiceName, serviceName).set(WebhookEventConfig::getEnable, false);
        return this.update(updateWrapper);
    }


    @Override
    public List<WebhookEventConfig> selectByEventCode(String code) {
        QueryWrapper<WebhookEventConfig> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().eq(WebhookEventConfig::getBusinessEventCode, code).eq( WebhookEventConfig::getEnable, true);
        return this.selectEntitysByWapper(queryWrapper);
    }


    @Override
    public WebhookEventConfig selectByCodeUrl(String code, String url) {
        QueryWrapper<WebhookEventConfig> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().eq(WebhookEventConfig::getBusinessEventCode, code).eq(WebhookEventConfig::getUrl, url);
        return this.selectEntityByWapper(queryWrapper);
    }


}