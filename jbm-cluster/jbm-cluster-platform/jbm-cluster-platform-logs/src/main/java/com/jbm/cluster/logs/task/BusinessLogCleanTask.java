package com.jbm.cluster.logs.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务日志清理定时任务（已废弃）
 * 
 * ⚠️ 注意：由于过期管理已托管给OpenObserve（通过流的保留策略TTL自动过期），
 * 此定时任务已不再需要。OpenObserve会根据流的保留策略自动删除过期数据。
 * 
 * 此任务仅保留用于业务层面的状态标记（如果需要），
 * 实际数据删除由OpenObserve自动完成，无需定时任务。
 * 
 * @deprecated 过期管理已由OpenObserve自动处理，此定时任务可以禁用或删除
 * @author wesley
 */
@Component
@Slf4j
public class BusinessLogCleanTask {
    
    /**
     * 清理过期业务日志（已废弃）
     * 
     * ⚠️ 已废弃：过期管理已由OpenObserve自动处理，此方法仅用于业务状态标记。
     * 如需禁用此定时任务，可以注释掉@Scheduled注解或删除整个类。
     * 
     * @deprecated 过期管理已由OpenObserve自动处理，无需定时任务
     */
    @Deprecated
    // @Scheduled(cron = "0 0 2 * * ?")  // 已禁用：过期管理由OpenObserve自动处理
    public void cleanExpiredLogs() {
        log.warn("⚠️ 定时清理任务已废弃：过期管理已由OpenObserve自动处理");
        log.info("提示：OpenObserve会根据流的保留策略（TTL）自动删除过期数据，无需手动清理");
        
        // 如果需要业务层面的状态标记，可以取消下面的注释并注入BusinessLogService
        /*
        @Autowired
        private BusinessLogService businessLogService;
        
        log.info("开始执行业务日志状态标记任务...");
        try {
            int count = businessLogService.cleanExpiredLogs();
            log.info("业务日志状态标记任务完成，共标记{}条（实际数据删除由OpenObserve自动完成）", count);
        } catch (Exception e) {
            log.error("业务日志状态标记任务执行失败", e);
        }
        */
    }
}

