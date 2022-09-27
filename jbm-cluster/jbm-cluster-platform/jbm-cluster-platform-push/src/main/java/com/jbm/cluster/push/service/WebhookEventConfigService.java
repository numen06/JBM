package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventResource;
import com.jbm.framework.masterdata.service.IMultiPlatformService;
import org.springframework.messaging.Message;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
public interface WebhookEventConfigService extends IMultiPlatformService<WebhookEventConfig> {


    List<WebhookEventConfig> selectByEventCode(String code);

    WebhookEventConfig selectByCodeUrl(String code, String url);
}
