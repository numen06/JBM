package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.api.event.TestBusinessEvent;
import com.jbm.cluster.api.event.annotation.BusinessEventListener;
import com.jbm.cluster.api.job.SchedulerJob;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.result.WebhookTaskReslut;
import com.jbm.cluster.push.service.WebhookTaskService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

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
        return ResultBody.success("监听测试-1成功");
    }

    @ApiOperation("监听测试-2")
    @BusinessEventListener(eventClass = TestBusinessEvent.class)
    @PostMapping("/businessEventListener2")
    public ResultBody<String> businessEventListener2(@RequestBody TestBusinessEvent testBusinessEvent) {
        return ResultBody.success("监听测试-2成功");
    }

    @ApiOperation("触发")
    @GetMapping("/run")
    public ResultBody run(String eventId) {
        return ResultBody.call("触发成功", () -> this.service.sendEvent(eventId));
    }

    @ApiOperation("请求")
    @PostMapping("/req")
    public ResultBody<Void> run(@RequestBody WebhookTask webhookTask) {
        return ResultBody.call("请求成功", () -> service.sendEvent(webhookTask));
    }

    @ApiOperation("重试")
    @GetMapping("/retry")
    public ResultBody<Void> retry(String taskId) {
        return ResultBody.call("重试成功", () -> service.retryEventTask(taskId));
    }

    @SchedulerJob(name = "每天零点清理多余的推送任务", cron = "0 0 0 * * ?")
    @ApiOperation("清理")
    @GetMapping("/clear")
    public ResultBody<Boolean> clear() {
        return ResultBody.callback("清理成功", () -> {
            return service.clearTasks();
        });
    }

    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/findTask")
    public ResultBody<WebhookTask> findConfig(@RequestBody(required = false) final WebhookTask entity) {
        return ResultBody.callback("查询成功", () -> {
            return service.selectEntity(entity);
        });
    }

    @ApiOperation(value = "查询任务列表")
    @PostMapping("/selectWebhookTasks")
    public ResultBody<DataPaging<WebhookTaskReslut>> selectWebhookTasks(@RequestBody(required = false) final WebhookTaskForm webhookTaskForm) {
        return ResultBody.callback("查询成功", () -> {
            return service.selectWebhookTasks(webhookTaskForm);
        });
    }
}
