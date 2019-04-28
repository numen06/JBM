package pres.lnk.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import pres.lnk.springframework.annotation.ScheduledCluster;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Author lnk
 * @Date 2019/3/26
 */
@Configuration
public class TaskConfig {

    /**
     * 测试同一个id的任务只有一个能执行
     */
    @ScheduledCluster("task")
    @Scheduled(cron = "0/1 * * * * ?")
    public void timedTask1() {
        System.out.println("task-1: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));
    }


    /**
     * @see #timedTask1()
     */
    @ScheduledCluster("task")
    @Scheduled(cron = "0/1 * * * * ?")
    public void timedTask2() {
        System.out.println("task-2: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));
    }

    /**
     * 不受集群控制，可以用来执行只处理当前服务器的业务，如：清理临时文件
     */
    @ScheduledCluster(id = "task", ignore = true)
    @Scheduled(cron = "0/5 * * * * ?")
    public void timedTask3() {
        System.out.println("不受集群控制: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));
    }
}
