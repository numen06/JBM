package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.cluster.push.service.PushMessageService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-11-19 16:21:00
 */
@Api(tags = "PushMessage开放接口")
@RestController
@RequestMapping("/pushMessage")
public class PushMessageController extends MasterDataCollection<PushMessage, PushMessageService> {
}
