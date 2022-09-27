package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.api.event.TestBusinessEvent;
import com.jbm.cluster.api.event.annotation.BusinessEventListener;
import com.jbm.cluster.push.service.WebhookTaskService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Api(tags = "Web反向推送开放接口")
@RestController
@RequestMapping("/webhookTask")
public class WebhookTaskController extends MultiPlatformCollection<WebhookTask, WebhookTaskService> {

    @ApiOperation("监听测试-1")
    @BusinessEventListener(eventClass = TestBusinessEvent.class)
    @PostMapping("/businessEventListener")
    public ResultBody<String> businessEventListener(@RequestBody TestBusinessEvent testBusinessEvent) {
        return ResultBody.success("标记已读成功");
    }

    @ApiOperation("监听测试-2")
    @BusinessEventListener(eventClass = TestBusinessEvent.class)
    @PostMapping("/businessEventListener2")
    public ResultBody<String> businessEventListener2(@RequestBody TestBusinessEvent testBusinessEvent) {
        return ResultBody.success("标记已读成功");
    }
}
