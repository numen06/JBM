package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.push.EmailPushConfig;
import com.jbm.cluster.push.service.EmailPushConfigService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-12-04 14:19:55
 */
@Api(tags = "消息推送项开放接口")
@RestController
@RequestMapping("/emailPushConfig")
public class EmailPushConfigController extends MasterDataCollection<EmailPushConfig, EmailPushConfigService> {
}
