package com.jbm.cluster.push.bevent.lis;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.bevent.TaskStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * webhook任务结束事件
 * @author wesley
 */
@Getter
public class WebhookTaskEndEvent extends ApplicationEvent {
    private final WebhookTask webhookTask;

    private final TaskStatus taskStatus;

    public WebhookTaskEndEvent(Object source, WebhookTask webhookTask, TaskStatus taskStatus) {
        super(source);
        this.webhookTask = webhookTask;
        this.taskStatus = taskStatus;
        webhookTask.setStatus(taskStatus.toString());
    }


    public static WebhookTaskEndEvent success(Object source, WebhookTask webhookTask) {
        return new WebhookTaskEndEvent(source, webhookTask, TaskStatus.SUCCESS);
    }

    public static WebhookTaskEndEvent failed(Object source, WebhookTask webhookTask) {
        return new WebhookTaskEndEvent(source, webhookTask, TaskStatus.FAILED);
    }
}

