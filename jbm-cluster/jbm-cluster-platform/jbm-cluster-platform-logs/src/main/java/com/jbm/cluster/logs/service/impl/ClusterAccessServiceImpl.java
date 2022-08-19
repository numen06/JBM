package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.logs.event.AccessEvent;
import com.jbm.cluster.logs.form.ClusterAccessInfo;
import com.jbm.cluster.logs.service.ClusterAccessService;
import com.jbm.cluster.logs.service.GatewayLogsService;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ClusterAccessServiceImpl implements ClusterAccessService {


    private ClusterAccessInfo clusterAccessInfo;

    private static final String ACCESS_CACHE_KEY = "jbm.cluster.cache.access";


    @Autowired
    private RedisService redisService;

    @Autowired
    private GatewayLogsService gatewayLogsService;


    @PostConstruct
    public void init() {
        this.load();
    }

    /**
     * 累加
     */
    @EventListener
    public void accumulate(AccessEvent accessEvent) {
        AtomicLong atomicTotal = new AtomicLong(clusterAccessInfo.getTotal());
        clusterAccessInfo.setTotal(atomicTotal.addAndGet(1));
        try {
            if (DateUtil.isSameDay(clusterAccessInfo.getTime(), DateTime.now())) {
                clusterAccessInfo.setToday(0L);
            }
        } catch (Exception e) {
            clusterAccessInfo.setToday(0L);
        }
        AtomicLong atomicToday = new AtomicLong(clusterAccessInfo.getToday());
        clusterAccessInfo.setToday(atomicToday.addAndGet(1));
        redisService.setCacheObject(ACCESS_CACHE_KEY, clusterAccessInfo);
    }


    public void load() {
        ClusterAccessInfo temp = redisService.getCacheObject(ACCESS_CACHE_KEY);
        if (ObjectUtil.isEmpty(temp)) {
            temp = new ClusterAccessInfo();
            temp.setTime(DateTime.now());
            temp.setToday(gatewayLogsService.todayAccess());
            temp.setTotal(gatewayLogsService.totalAccess());
        }
        if (ObjectUtil.isNotEmpty(temp)) {
            clusterAccessInfo = temp;
        }
    }


    @Override
    public ClusterAccessInfo getClusterAccessInfo() {
        return this.clusterAccessInfo;
    }
}
