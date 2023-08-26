package com.jbm.cluster.common.health;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DependsOnHealth extends AbstractScheduledService {
    //检查周期
    private final static Long CHECK_TIME = 2L;
    //字典清单
    private DiscoveryClient discoveryClient;
    //启动程序
    private ApplicationContext applicationContext;
    //启动时间
    private Date startTime;


    private List<String> dependsOnList = new ArrayList<>();


    public DependsOnHealth(DiscoveryClient discoveryClient, ApplicationContext applicationContext,  String... dependsOnArr) {
        this.discoveryClient = discoveryClient;
        this.applicationContext = applicationContext;
        if (ArrayUtil.isNotEmpty(dependsOnArr)) {
            this.dependsOnList = CollUtil.newArrayList(dependsOnArr);
        }
    }

    @PostConstruct
    public void init() {
        if (CollUtil.isEmpty(this.dependsOnList)) {
            return;
        }
        //记录启动时间
        startTime = DateTime.now();
        log.info("开始启动检查程序");
        //开始异步守护程序
        this.startAsync();
    }


    @Override
    protected void runOneIteration() throws Exception {
        Date now = DateTime.now();
        if (DateUtil.between(startTime, now, DateUnit.MINUTE, true) >= CHECK_TIME) {
            log.info("超过2分钟,停止检查为分布式程序");
            //超过2分钟停止监听
            this.stopAsync();
            return;
        }
        for (String service : this.dependsOnList) {
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(service);
            //没有检测到程序
            if (CollUtil.isEmpty(serviceInstances)) {
                log.info("未检测到依赖程序,停止应用程序");
                System.exit(SpringApplication.exit(applicationContext));
            }
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(5, 5, TimeUnit.SECONDS);
    }
}
