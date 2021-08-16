package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.entity.message.PushMessage;
import com.jbm.cluster.push.service.PushMessageService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-05 03:05:23
 */
@Api(tags = "站内信接口")
@RestController
@RequestMapping("/pushMessage")
public class PushMessageController extends MasterDataCollection<PushMessage, PushMessageService> {
}
