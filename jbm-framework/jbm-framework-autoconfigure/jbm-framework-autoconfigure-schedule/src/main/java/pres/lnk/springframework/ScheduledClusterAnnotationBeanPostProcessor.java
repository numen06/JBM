package pres.lnk.springframework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.StringValueResolver;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;

/**
 * 定时任务处理器
 * 对定时任务添加了集群系统下的执行控制
 *
 * @Author lnk
 * @Date 2018/2/28
 */
public class ScheduledClusterAnnotationBeanPostProcessor extends ScheduledAnnotationBeanPostProcessor implements SchedulingConfigurer {
    private static Logger logger = LoggerFactory.getLogger(ScheduledClusterAnnotationBeanPostProcessor.class);

    private StringValueResolver embeddedValueResolver;

    /** @see #level(int) */
    @Value(value = "${spring.scheduling.cluster.level:}")
    private Integer level;

    /** @see #heartTime(int) */
    @Value(value = "${spring.scheduling.cluster.heartTime:60}")
    private Integer heartTime = 60;

    @Autowired
    private AbstractScheduler scheduler;

    @Override
    protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
        try {
            bean = new ScheduledMethodInvoker(bean, method, scheduler, embeddedValueResolver);
            method = ScheduledMethodInvoker.class.getMethod("invoke");
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
            return;
        }
        super.processScheduled(scheduled, method, bean);
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        super.setEmbeddedValueResolver(resolver);
        embeddedValueResolver = resolver;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (level != null) {
            //当前设置的服务器执行优先级别时，定时告诉中间件当前服务器还活着
            scheduler.setLevel(level);
            scheduler.setHeartTime(heartTime);
            scheduler.keepAlive();

            taskRegistrar.addFixedRateTask(new IntervalTask(new Runnable() {
                @Override
                public void run() {
                    scheduler.keepAlive();
                }

            }, heartTime * 1000, heartTime * 1000));
        }
    }

    /**
     * 设置服务器的执行优先级别
     * 优先级别从0开始设置
     * 其中0为关闭定时任务，设置0时，该服务器实例的定时任务不会执行
     * 然后1级别最高，数字越大则级别越小。当高级别的服务器实例还在运行时，低级别的服务器的定时任务则不会执行，当高级别服务器挂了，低级别的定时任务才会执行
     *
     * @param level 执行优先级别
     * @return
     */
    public ScheduledClusterAnnotationBeanPostProcessor level(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("level 不能低于0");
        }
        this.level = level;
        return this;
    }

    /**
     * 设置服务器的心跳时间，定时告诉中间件服务器还在运行
     *
     * @param heartTime 心跳时间（秒）
     * @return
     */
    public ScheduledClusterAnnotationBeanPostProcessor heartTime(int heartTime) {
        if (level < 0) {
            throw new IllegalArgumentException("heartTime 不能低于0");
        }
        this.heartTime = heartTime;
        return this;
    }
}