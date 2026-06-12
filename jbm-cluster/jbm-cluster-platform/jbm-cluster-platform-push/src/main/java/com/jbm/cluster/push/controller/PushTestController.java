package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.push.model.PushTestAck;
import com.jbm.cluster.push.model.PushTestRequest;
import com.jbm.cluster.push.model.PushTestTaskStatus;
import com.jbm.cluster.push.service.PushTestService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Api(tags = "Push通讯测试")
@RestController
@RequestMapping("/pushTest")
public class PushTestController {

    @Autowired
    private PushTestService pushTestService;

    @ApiOperation("发送单次测试消息")
    @PostMapping("/send")
    public ResultBody<PushTestTaskStatus> send(@RequestBody PushTestRequest request) {
        assertAllowed();
        return ResultBody.success(pushTestService.send(request), "测试消息已发送");
    }

    @ApiOperation("启动轻量性能测试")
    @PostMapping("/perf")
    public ResultBody<PushTestTaskStatus> perf(@RequestBody PushTestRequest request) {
        assertAllowed();
        return ResultBody.success(pushTestService.startPerf(request), "轻压测任务已启动");
    }

    @ApiOperation("查询测试任务")
    @GetMapping("/perf/{taskId}")
    public ResultBody<PushTestTaskStatus> status(@PathVariable String taskId) {
        assertAllowed();
        return ResultBody.success(pushTestService.getStatus(taskId), "查询测试任务成功");
    }

    @ApiOperation("WebSocket测试消息ACK")
    @PostMapping("/ack")
    public ResultBody<PushTestTaskStatus> ack(@RequestBody PushTestAck ack) {
        return ResultBody.success(pushTestService.ack(ack), "ACK成功");
    }

    private void assertAllowed() {
        if (LoginHelper.isAdmin()) {
            return;
        }
        JbmLoginUser user = LoginHelper.softGetLoginUser();
        Set<String> authorities = user == null ? null : user.getAuthorities();
        if (authorities != null && (authorities.contains("push_test")
                || authorities.contains("MENU_push_test")
                || authorities.contains("ACTION_push:test"))) {
            return;
        }
        throw new ServiceException("无权访问Push通讯测试");
    }
}
