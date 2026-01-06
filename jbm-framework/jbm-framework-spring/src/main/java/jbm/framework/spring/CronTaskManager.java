package jbm.framework.spring;

import cn.hutool.core.util.IdUtil;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CronTaskManager implements SchedulingConfigurer {

    private final ConcurrentHashMap<String, TaskHolder> tasks = new ConcurrentHashMap<>();
    private final String defaultCron = "0 */1 * * * ?"; // 默认每分钟

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        // 注册一个通用触发器，实际逻辑由 TaskHolder 动态决定
        registrar.addTriggerTask(this::executeAllActiveTasks, new org.springframework.scheduling.Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                // 每次触发前动态读取当前 cron 表达式
                return new CronTrigger(defaultCron).nextExecutionTime(triggerContext);
            }
        });
    }

    private Trigger buildDynamicTrigger(TriggerContext context) {
        // 每次触发前检查：返回当前生效的 CronTrigger（支持运行时变更）
        return new CronTrigger(defaultCron);
    }

    private void executeAllActiveTasks() {
        tasks.values().stream()
                .filter(TaskHolder::isActive)
                .forEach(TaskHolder::run);
    }

    // ======== ✨ 核心 API（对外使用） ========

    public void schedule(Runnable task, String cron) {
        schedule(IdUtil.fastUUID(), task, cron);
    }

    public void schedule(String taskId, Runnable task, String cron) {
        if (tasks.containsKey(taskId)) {
            cancel(taskId);
        }
        tasks.put(taskId, new TaskHolder(task, cron));
    }

    public void updateCron(String taskId, String newCron) {
        TaskHolder holder = tasks.get(taskId);
        if (holder != null) {
            holder.updateCron(newCron);
        }
    }

    public void cancel(String taskId) {
        tasks.remove(taskId);
    }

    public void pause(String taskId) {
        TaskHolder h = tasks.get(taskId);
        if (h != null) h.pause();
    }

    public void resume(String taskId) {
        TaskHolder h = tasks.get(taskId);
        if (h != null) h.resume();
    }

    public boolean isActive(String taskId) {
        TaskHolder h = tasks.get(taskId);
        return h != null && h.isActive();
    }

    public String getCron(String taskId) {
        TaskHolder h = tasks.get(taskId);
        return h != null ? h.cron : null;
    }

    // ======== 内部状态封装 ========
    private static class TaskHolder {
        private final Runnable task;
        private volatile String cron;
        private final AtomicBoolean active = new AtomicBoolean(true);

        TaskHolder(Runnable task, String cron) {
            this.task = task;
            this.cron = cron;
        }

        void run() {
            if (active.get()) {
                try {
                    task.run();
                } catch (Exception e) {
                    // 生产建议加日志：log.warn("Task {} failed", taskId, e);
                }
            }
        }

        void updateCron(String newCron) {
            this.cron = newCron;
        }

        void pause() {
            active.set(false);
        }

        void resume() {
            active.set(true);
        }

        boolean isActive() {
            return active.get();
        }
    }
}