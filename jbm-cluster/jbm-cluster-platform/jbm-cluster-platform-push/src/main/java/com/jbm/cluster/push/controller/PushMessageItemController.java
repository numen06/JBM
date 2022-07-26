package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Api(tags = "消息推送项开放接口")
@RestController
@RequestMapping("/pushMessageItem")
public class PushMessageItemController extends MasterDataCollection<PushMessageItem, PushMessageItemService> {


}
