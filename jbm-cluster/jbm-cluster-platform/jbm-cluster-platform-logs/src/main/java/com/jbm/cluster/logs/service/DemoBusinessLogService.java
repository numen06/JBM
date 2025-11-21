package com.jbm.cluster.logs.service;

import com.jbm.cluster.common.basic.module.JbmBusinessLogTemplate;
import lombok.extern.slf4j.Slf4j;
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
     * 执行演示任务
     * 模拟一个真实的数据导入处理流程
     * 使用JbmBusinessLogTemplate记录日志，完全模拟真实使用情况
     * 
     * @param logId 业务日志ID
     */
    public void executeDemo(String logId) {
        try {
            // 使用JbmBusinessLogTemplate的withLogContext方式，确保整个流程在同一个日志上下文中
            JbmBusinessLogTemplate.withLogContext(builder -> {
                builder.logId(logId)
                        .businessId(logId)
                        .businessType("DEMO")
                        .source("business-log-demo")
                        .expireDays(7)
                        .autoTimestamp(true);
            }, () -> {
                try {
                    // 初始化阶段（使用静态方法生成格式化的字符串）
                    log.info(JbmBusinessLogTemplate.stageInit("prepare,准备资源,1;process,处理数据,2;archive,归档输出,3"));
                    
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
        log.info(JbmBusinessLogTemplate.stageUpdate("prepare", "RUNNING", 20, "开始准备基础资源...", 10));
        Thread.sleep(800);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("prepare", "RUNNING", 50, "检查系统环境配置", 15));
        Thread.sleep(600);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("prepare", "DONE", 100, "资源准备完毕，共分配3个处理线程", 25));
    }

    /**
     * 执行处理阶段
     * 阶段信息中包含业务日志内容，避免重复
     */
    private void executeProcessStage() throws InterruptedException {
        log.info(JbmBusinessLogTemplate.stageUpdate("process", "RUNNING", 10, "开始批量处理数据...", 30));
        Thread.sleep(500);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("process", "RUNNING", 30, "读取数据文件，共1000条记录", 40));
        Thread.sleep(600);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("process", "RUNNING", 60, "数据校验完成，通过记录：980条，失败记录：20条", 60));
        Thread.sleep(700);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("process", "RUNNING", 85, "开始写入数据库，批量插入中...", 70));
        Thread.sleep(800);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("process", "DONE", 100, "数据处理完成，成功导入980条记录", 75));
    }

    /**
     * 执行归档阶段
     * 阶段信息中包含业务日志内容，避免重复
     */
    private void executeArchiveStage() throws InterruptedException {
        log.info(JbmBusinessLogTemplate.stageUpdate("archive", "RUNNING", 20, "开始归档生成报告...", 80));
        Thread.sleep(500);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("archive", "RUNNING", 60, "生成处理统计报告", 90));
        Thread.sleep(600);
        
        log.info(JbmBusinessLogTemplate.stageUpdate("archive", "DONE", 100, "归档完成，任务结束。总耗时：3.2秒", 100));
    }
}

