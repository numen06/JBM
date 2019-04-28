package pres.lnk.springframework;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import pres.lnk.springframework.annotation.ScheduledCluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * 定时任务执行器
 *
 * @Author lnk
 * @Date 2018/2/28
 */
public class ScheduledMethodInvoker {

    /**
     * 任务对象
     */
    private final Object target;

    /**
     * 定时任务的执行方法
     */
    private final Method method;

    /**
     * 任务id，用来做锁
     */
    private String taskId;

    /**
     * 是否忽略集群控制
     *
     * @see ScheduledCluster#ignore()
     */
    private boolean ignore = false;

    /**
     * 是否忽略集群控制
     *
     * @see ScheduledCluster#description()
     */
    private String description;


    /**
     * 定时任务调度器
     */
    private AbstractScheduler scheduler;

    private StringValueResolver embeddedValueResolver;

    public ScheduledMethodInvoker(Object target, Method method, AbstractScheduler scheduler, StringValueResolver embeddedValueResolver) {
        this.target = target;
        this.method = AopUtils.selectInvocableMethod(method, target.getClass());
        this.scheduler = scheduler != null ? scheduler : new LocalSchedulerImpl();
        this.embeddedValueResolver = embeddedValueResolver;

        init();
    }

    public void invoke() {
        long startTimeMillis = 0, endTimeMillis = 0;
        try {
            int maxLevel = scheduler.getMaxAliveLevel();
            if (ignore || maxLevel < 0) {
                //设置该任务不受集群调试器控制，直接执行
                ReflectionUtils.makeAccessible(this.method);
                this.method.invoke(this.target);
                return;
            }

            if (scheduler.getLevel() == 0 || scheduler.getLevel() > maxLevel) {
                //如果当前服务器的级别是0，或比当前运行中最高级别的服务器低，则不执行任务
                scheduler.setStatus(AbstractScheduler.FAIL_LEVEL);
                return;
            } else if (scheduler.getLevel() < maxLevel) {
                //如果当前服务器比最高级别还要高，则修改中间件的最高级别
                scheduler.keepAlive();
            }

            //检测任务是否可执行
            if (scheduler.check(taskId)) {
                Scheduled scheduled = method.getAnnotation(Scheduled.class);
                //获取任务下次执行的间隔时长，然后获取任务锁，如果获取成功则执行任务
                long timeoutMillis = ScheduledUtil.getNextTimeInterval(scheduled, embeddedValueResolver);
                //减500毫秒是为了解决jdk定时任务存在误差问题，防止下次任务执行时间时间还没到而跳过本次任务
                timeoutMillis -= 500;
                if (scheduler.lock(taskId, timeoutMillis)) {
                    //获取锁成功，执行任务
                    startTimeMillis = scheduler.currentTimeMillis();
                    ReflectionUtils.makeAccessible(this.method);
                    this.method.invoke(this.target);
                    endTimeMillis = scheduler.currentTimeMillis();
                    scheduler.setStatus(AbstractScheduler.SUCCESS);

                    if (ScheduledUtil.SCHEDULED_FIXED_DELAY.equals(ScheduledUtil.getType(scheduled))) {
                        //fixedDelay 为任务执行结束再计算下次执行时间
                        timeoutMillis = ScheduledUtil.getNextTimeInterval(scheduled, embeddedValueResolver);
                        scheduler.relock(taskId, timeoutMillis);
                    }
                } else {
                    scheduler.setStatus(AbstractScheduler.FAIL_LOCK);
                }
            } else {
                scheduler.setStatus(AbstractScheduler.FAIL_CHECK);
            }
        } catch (Exception ex) {
            scheduler.setException(ex);
        } finally {
            try {
                scheduler.executed(this.method, this.target, startTimeMillis, endTimeMillis, description);
            } catch (InvocationTargetException ex) {
                ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
            } catch (IllegalAccessException ex) {
                throw new UndeclaredThrowableException(ex);
            } catch (Exception ex) {
                ReflectionUtils.rethrowRuntimeException(ex);
            }
            scheduler.reStatus();
        }

    }

    public void init() {
        ScheduledCluster ds = AnnotationUtils.getAnnotation(method, ScheduledCluster.class);
        if (ds != null) {
            if (StringUtils.hasText(ds.id())) {
                taskId = ds.id();
            }
            ignore = ds.ignore();
            description = ds.description();
        }
        if (StringUtils.isEmpty(taskId)) {
            taskId = method.getDeclaringClass().getCanonicalName() + "." + method.getName();
        }
//        Scheduled scheduled = method.getAnnotation(Scheduled.class);
//        String flag = ScheduledUtil.getFlag(scheduled, embeddedValueResolver);
//        taskId += "_" + flag;
//        taskId = taskId.replaceAll("\\W", "_");
    }
}
