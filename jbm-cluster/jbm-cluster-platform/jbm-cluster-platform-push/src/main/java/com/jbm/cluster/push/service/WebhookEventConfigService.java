package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.framework.masterdata.service.IMultiPlatformService;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
public interface WebhookEventConfigService extends IMultiPlatformService<WebhookEventConfig> {


    WebhookEventConfig selectByEventId(String eventId);

    boolean deleteOldBatch(String serviceName,String batchTime);

    List<WebhookEventConfig> selectByEventCode(String code);

    WebhookEventConfig selectByCodeUrl(String code, String url);
}
