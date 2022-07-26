package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.push.service.PushMessageBodyService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Api(tags = "推送消息内容开放接口")
@RestController
@RequestMapping("/pushMessageBody")
public class PushMessageBodyController extends MasterDataCollection<PushMessageBody, PushMessageBodyService> {


}
