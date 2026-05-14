package com.jbm.cluster.push.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 推送服务异步线程池：替代默认 SimpleAsyncTaskExecutor，并为通知分发提供独立池。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String NOTIFICATION_DISPATCHER_EXECUTOR = "notificationDispatcherExecutor";

    private volatile ThreadPoolExecutor notificationDispatcherRawPool;

    @Bean(name = "taskExecutor")
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores * 2);
        executor.setMaxPoolSize(cores * 4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("push-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = NOTIFICATION_DISPATCHER_EXECUTOR)
    public ExecutorService notificationDispatcherExecutor() {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        notificationDispatcherRawPool = new ThreadPoolExecutor(
                cores,
                cores * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new CustomizableThreadFactory("notification-dispatch-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return notificationDispatcherRawPool;
    }

    @PreDestroy
    public void shutdownNotificationDispatcherPool() {
        if (notificationDispatcherRawPool != null) {
            notificationDispatcherRawPool.shutdown();
            try {
                if (!notificationDispatcherRawPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    notificationDispatcherRawPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                notificationDispatcherRawPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
