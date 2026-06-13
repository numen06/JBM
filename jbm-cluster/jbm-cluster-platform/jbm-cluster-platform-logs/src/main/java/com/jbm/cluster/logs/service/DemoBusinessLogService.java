package com.jbm.cluster.logs.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * 业务日志演示服务
 * 使用JbmBusinessLogTemplate模拟真实的业务场景
 * 阶段信息直接通过log.info输出，日志收集器会解析并反馈
 * 
 * @author wesley
 */
@Service
@Slf4j
public class DemoBusinessLogService {

    /**
     * 执行简单日志演示（模式1：最简单的日志收集）
     * 只记录普通日志，不涉及阶段跟踪
     * 
     * @param logId 业务日志ID
     */
    public void executeSimpleDemo(String logId) {
        try {
            withDemoLogContext(logId, () -> {
                try {
                    log.info("=== 简单日志演示开始 ===");
                    log.info("开始执行任务...");
                    Thread.sleep(500);
                    
                    log.info("步骤1：初始化系统配置");
                    Thread.sleep(400);
                    
                    log.info("步骤2：加载数据文件");
                    Thread.sleep(500);
                    
                    log.info("步骤3：处理业务逻辑");
                    Thread.sleep(600);
                    
                    log.info("步骤4：保存处理结果");
                    Thread.sleep(400);
                    
                    log.info("=== 任务执行完成 ===");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("演示任务被中断", e);
                } catch (Exception e) {
                    log.error("演示任务执行异常", e);
                }
            });
        } catch (Exception e) {
            log.error("演示任务执行失败，logId={}", logId, e);
        }
    }

    /**
     * 执行单阶段进度跟踪演示（模式2：单阶段进度跟踪）
     * 只有一个阶段，展示进度百分比
     * 
     * @param logId 业务日志ID
     */
    public void executeSingleStageDemo(String logId) {
        try {
            withDemoLogContext(logId, () -> {
                try {
                    // 初始化单个阶段
                    log.info(stageInit("process,数据处理,1"));
                    
                    log.info("开始处理数据...");
                    Thread.sleep(500);
                    
                    log.info(stageUpdate("process", "RUNNING", 20, "读取数据文件", 20));
                    Thread.sleep(600);
                    
                    log.info(stageUpdate("process", "RUNNING", 40, "数据校验中...", 40));
                    Thread.sleep(700);
                    
                    log.info(stageUpdate("process", "RUNNING", 60, "开始处理业务逻辑", 60));
                    Thread.sleep(800);
                    
                    log.info(stageUpdate("process", "RUNNING", 80, "保存处理结果", 80));
                    Thread.sleep(600);
                    
                    log.info(stageUpdate("process", "DONE", 100, "数据处理完成，共处理1000条记录", 100));
                    log.info("任务执行完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("演示任务被中断", e);
                } catch (Exception e) {
                    log.error("演示任务执行异常", e);
                }
            });
        } catch (Exception e) {
            log.error("演示任务执行失败，logId={}", logId, e);
        }
    }

    /**
     * 执行多阶段进度跟踪演示（模式3：复杂的多阶段进度跟踪）
     * 多个阶段，每个阶段有独立的进度，同时有整体进度
     * 
     * @param logId 业务日志ID
     */
    public void executeMultiStageDemo(String logId) {
        try {
            withDemoLogContext(logId, () -> {
                try {
                    // 初始化多个阶段
                    log.info(stageInit("prepare,准备资源,1;process,处理数据,2;archive,归档输出,3"));
                    
                    // 阶段1：准备资源
                    executePrepareStage();
                    
                    // 阶段2：处理数据
                    executeProcessStage();
                    
                    // 阶段3：归档输出
                    executeArchiveStage();
                    
                    log.info("演示任务执行完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("演示任务被中断", e);
                } catch (Exception e) {
                    log.error("演示任务执行异常", e);
                }
            });
        } catch (Exception e) {
            log.error("演示任务执行失败，logId={}", logId, e);
        }
    }

    /**
     * 执行准备阶段
     * 阶段信息中包含业务日志内容，避免重复
     */
    private void executePrepareStage() throws InterruptedException {
        log.info(stageUpdate("prepare", "RUNNING", 20, "开始准备基础资源...", 10));
        Thread.sleep(800);
        
        log.info(stageUpdate("prepare", "RUNNING", 50, "检查系统环境配置", 15));
        Thread.sleep(600);
        
        log.info(stageUpdate("prepare", "DONE", 100, "资源准备完毕，共分配3个处理线程", 25));
    }

    /**
     * 执行处理阶段
     * 阶段信息中包含业务日志内容，避免重复
     */
    private void executeProcessStage() throws InterruptedException {
        log.info(stageUpdate("process", "RUNNING", 10, "开始批量处理数据...", 30));
        Thread.sleep(500);
        
        log.info(stageUpdate("process", "RUNNING", 30, "读取数据文件，共1000条记录", 40));
        Thread.sleep(600);
        
        log.info(stageUpdate("process", "RUNNING", 60, "数据校验完成，通过记录：980条，失败记录：20条", 60));
        Thread.sleep(700);
        
        log.info(stageUpdate("process", "RUNNING", 85, "开始写入数据库，批量插入中...", 70));
        Thread.sleep(800);
        
        log.info(stageUpdate("process", "DONE", 100, "数据处理完成，成功导入980条记录", 75));
    }

    /**
     * 执行归档阶段
     * 阶段信息中包含业务日志内容，避免重复
     */
    private void executeArchiveStage() throws InterruptedException {
        log.info(stageUpdate("archive", "RUNNING", 20, "开始归档生成报告...", 80));
        Thread.sleep(500);
        
        log.info(stageUpdate("archive", "RUNNING", 60, "生成处理统计报告", 90));
        Thread.sleep(600);
        
        log.info(stageUpdate("archive", "DONE", 100, "归档完成，任务结束。总耗时：3.2秒", 100));
    }

    private void withDemoLogContext(String logId, Runnable runnable) {
        try {
            MDC.put("businessLogId", logId);
            MDC.put("traceId", logId);
            MDC.put("businessType", "DEMO");
            MDC.put("businessId", logId);
            MDC.put("source", "business-log-demo");
            MDC.put("expireDays", "7");
            MDC.put("autoTimestamp", "true");
            runnable.run();
        } finally {
            MDC.remove("businessLogId");
            MDC.remove("traceId");
            MDC.remove("businessType");
            MDC.remove("businessId");
            MDC.remove("source");
            MDC.remove("expireDays");
            MDC.remove("autoTimestamp");
        }
    }

    private String stageInit(String stages) {
        return "[STAGE:INIT:" + stages + "]";
    }

    private String stageUpdate(String stageCode, String status, int progress, String message, Integer overallProgress) {
        String stageInfo = stageCode + "," + status + "," + progress + "," + message;
        if (overallProgress != null) {
            stageInfo += "," + overallProgress;
        }
        return "[STAGE:UPDATE:" + stageInfo + "]";
    }
}
