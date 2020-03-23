package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.message.PushMessage;
import com.jbm.cluster.center.service.PushMessageService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-24 02:53:33
 */
@Api(tags = "PushMessage开放接口")
@RestController
@RequestMapping("/pushMessage")
public class PushMessageController extends MasterDataCollection<PushMessage, PushMessageService> {
}
