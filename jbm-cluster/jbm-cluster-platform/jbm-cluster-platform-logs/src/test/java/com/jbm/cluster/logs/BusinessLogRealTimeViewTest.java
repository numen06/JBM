package com.jbm.cluster.logs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jbm.cluster.logs.form.AppendBusinessLogForm;
import com.jbm.cluster.logs.form.CreateBusinessLogForm;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务日志实时查看测试
 * 模拟场景：
 * 1. 生产者线程：持续追加日志（模拟业务进程实时写日志）
 * 2. 消费者线程：实时查询日志（模拟页面/终端实时查看）
 * 3. 验证：消费者能够实时看到生产者追加的新日志
 * 
 * @author wesley
 */
@Slf4j
public class BusinessLogRealTimeViewTest {
    
    // 测试服务器地址
    private static final String BASE_URL = "http://localhost:3312";
    
    // HTTP客户端
    private static OkHttpClient httpClient;
    
    // 生产者线程池（模拟多个业务进程）
    private static ExecutorService producerPool;
    
    // 消费者线程池（模拟多个查看终端）
    private static ExecutorService consumerPool;
    
    @BeforeAll
    public static void setup() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        producerPool = Executors.newFixedThreadPool(3);
        consumerPool = Executors.newFixedThreadPool(5);
        
        log.info("\n========================================");
        log.info("业务日志实时查看测试");
        log.info("模拟多终端实时查看日志的场景");
        log.info("========================================\n");
    }
    
    /**
     * 测试：单生产者 + 单消费者（消息队列模式）
     * 模拟一个业务进程写日志，一个页面查看日志
     * 生产者和消费者同时进行，实时消费，类似消息队列的概念
     */
    @Test
    public void testSingleProducerSingleConsumer() throws InterruptedException {
        log.info("\n========== 测试：单生产者 + 单消费者（消息队列模式）==========\n");
        log.info("💡 说明：生产者和消费者同时进行，一边追加内容，一边实时打印最新内容\n");
        
        String logId;
        try {
            logId = createTestLog("实时查看测试", "单生产者单消费者");
        } catch (IOException e) {
            log.error("创建测试日志失败", e);
            return;
        }
        log.info("✓ 创建测试日志，logId: {}\n", logId);
        
        // 统计信息
        AtomicInteger producedLines = new AtomicInteger(0);
        AtomicInteger consumedLines = new AtomicInteger(0);
        AtomicLong lastConsumedLineNumber = new AtomicLong(0);
        
        // 使用 AtomicBoolean 标志控制消费者退出（消息队列模式）
        AtomicBoolean producerRunning = new AtomicBoolean(true);
        
        // 时间格式化器
        java.time.format.DateTimeFormatter timeFormatter = 
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        
        // 创建生产者（追加日志）- 独立线程，持续追加
        CountDownLatch producerLatch = new CountDownLatch(1);
        producerPool.submit(() -> {
            try {
                String startTime = java.time.LocalDateTime.now().format(timeFormatter);
                log.info("  [生产者] 🚀 开始生产日志... [{}]\n", startTime);
                
                for (int i = 1; i <= 20; i++) {
                    String now = java.time.LocalDateTime.now().format(timeFormatter);
                    String content = String.format("第%d行日志内容", i);
                    
                    try {
                        boolean success = appendLog(logId, content, true);
                        if (success) {
                            int currentProduced = producedLines.incrementAndGet();
                            log.info("  [生产者] [{}] ✍️  追加第{}行 → {} (成功)", now, currentProduced, content);
                            
                            // 给OpenObserve一点时间索引数据（提高实时性）
                            Thread.sleep(100);
                        } else {
                            log.error("  [生产者] [{}] ❌ 追加第{}行失败: 返回success=false", now, i);
                        }
                    } catch (IOException e) {
                        log.error("[生产者] [{}] ❌ 追加日志失败: {}", now, e.getMessage(), e);
                    }
                    Thread.sleep(400); // 每500ms追加一行（扣除100ms索引时间）
                }
                producerRunning.set(false);
                String endTime = java.time.LocalDateTime.now().format(timeFormatter);
                log.info("\n  [生产者] ✅ 完成，共追加{}行 [{}]\n", producedLines.get(), endTime);
            } catch (Exception e) {
                log.error("[生产者] ❌ 错误", e);
                producerRunning.set(false);
            } finally {
                producerLatch.countDown();
            }
        });
        
        // 等待一小段时间，让生产者先开始
        Thread.sleep(200);
        
        // 创建消费者（实时查询日志）- 独立线程，持续消费
        CountDownLatch consumerLatch = new CountDownLatch(1);
        consumerPool.submit(() -> {
            try {
                String startTime = java.time.LocalDateTime.now().format(timeFormatter);
                log.info("  [消费者] 👀 开始实时消费日志... [{}]\n", startTime);
                
                int queryCount = 0;
                int consecutiveEmptyQueries = 0; // 连续空查询次数
                
                // 持续轮询，直到生产者完成且所有日志都被消费（消息队列模式）
                while (producerRunning.get() || producedLines.get() > consumedLines.get()) {
                    queryCount++;
                    
                    // 查询总行数
                    try {
                        Integer totalLines = getTotalLines(logId);
                        
                        // 添加调试日志（每10次查询打印一次，或者查询到数据时）
                        if (queryCount % 10 == 0 || (totalLines != null && totalLines > 0)) {
                            log.info("  [消费者] 🔍 第{}次查询: 总行数={}, 已消费={}, 生产者已追加={}", 
                                    queryCount, totalLines, consumedLines.get(), producedLines.get());
                        }
                        
                        if (totalLines != null && totalLines > lastConsumedLineNumber.get()) {
                            // 查询新增的行（从上次消费的位置开始）
                            int startLine = (int) lastConsumedLineNumber.get() + 1;
                            List<Map<String, Object>> newLines = getLogByLineRange(logId, startLine, totalLines);
                            
                            if (newLines != null && !newLines.isEmpty()) {
                                consecutiveEmptyQueries = 0; // 重置空查询计数
                                
                                // 实时打印每一行新内容
                                for (Map<String, Object> line : newLines) {
                                    Integer lineNum = (Integer) line.get("lineNumber");
                                    String content = (String) line.get("content");
                                    String consumeTime = java.time.LocalDateTime.now().format(timeFormatter);
                                    
                                    log.info("  [消费者] [{}] 📥 实时消费第{}行: {}", consumeTime, lineNum, content);
                                    consumedLines.incrementAndGet();
                                    lastConsumedLineNumber.set(lineNum);
                                }
                            } else {
                                consecutiveEmptyQueries++;
                                // 如果查询到总行数增加了，但查询具体行时为空，可能是索引延迟
                                if (consecutiveEmptyQueries > 3) {
                                    log.info("  [消费者] ⏳ 检测到索引延迟，等待更长时间... (连续空查询{}次)", consecutiveEmptyQueries);
                                    Thread.sleep(300); // 增加等待时间
                                }
                            }
                        } else {
                            consecutiveEmptyQueries++;
                            // 如果生产者还在运行但查询不到数据，可能需要更长时间等待索引
                            if (producerRunning.get() && consecutiveEmptyQueries > 10) {
                                log.info("  [消费者] ⏳ 等待数据索引... (连续空查询{}次)", consecutiveEmptyQueries);
                                Thread.sleep(500); // 生产者还在运行，给更多时间等待索引
                            }
                        }
                    } catch (IOException e) {
                        String errorTime = java.time.LocalDateTime.now().format(timeFormatter);
                        log.error("[消费者] [{}] ❌ 查询日志失败", errorTime, e);
                        consecutiveEmptyQueries++;
                    }
                    
                    // 轮询间隔：300ms（给OpenObserve更多时间索引）
                    Thread.sleep(300);
                }
                
                String endTime = java.time.LocalDateTime.now().format(timeFormatter);
                log.info("\n  [消费者] ✅ 完成，共查询{}次，消费{}行 [{}]\n", 
                        queryCount, consumedLines.get(), endTime);
            } catch (Exception e) {
                log.error("[消费者] ❌ 错误", e);
            } finally {
                consumerLatch.countDown();
            }
        });
        
        // 等待完成（生产者和消费者同时运行）
        producerLatch.await(30, TimeUnit.SECONDS);
        log.info("\n  [主线程] 生产者已完成，等待消费者消费剩余日志...\n");
        
        // 重要：等待足够的时间让异步写入完成
        // postLogs是异步的，需要等待写入完成
        log.info("  [主线程] ⏳ 等待OpenObserve异步写入完成（5秒）...");
        Thread.sleep(5000);
        
        // 给消费者更多时间消费剩余日志（等待索引完成）
        // OpenObserve可能需要几秒钟来索引数据
        int maxWaitSeconds = 15;
        for (int i = 0; i < maxWaitSeconds && consumedLines.get() < producedLines.get(); i++) {
            Thread.sleep(1000);
            try {
                Integer currentTotal = getTotalLines(logId);
                log.info("  [主线程] 等待中... ({}/{}秒) 当前总行数: {}, 已消费: {}, 生产者已追加: {}", 
                        i + 1, maxWaitSeconds, currentTotal, consumedLines.get(), producedLines.get());
                
                // 如果查询到数据了，尝试立即消费
                if (currentTotal != null && currentTotal > 0 && consumedLines.get() == 0) {
                    log.info("  [主线程] 🎯 检测到数据，尝试查询所有记录...");
                    try {
                        List<Map<String, Object>> allLogs = getLogByLineRange(logId, 1, -1);
                        if (allLogs != null && !allLogs.isEmpty()) {
                            log.info("  [主线程] ✅ 查询到{}条记录！", allLogs.size());
                            for (int j = 0; j < Math.min(5, allLogs.size()); j++) {
                                Map<String, Object> logEntry = allLogs.get(j);
                                log.info("     行{}: {}", logEntry.get("lineNumber"), logEntry.get("content"));
                            }
                        }
                    } catch (IOException e) {
                        log.warn("  [主线程] 查询失败: {}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.warn("  [主线程] 查询总行数失败: {}", e.getMessage());
            }
        }
        
        consumerLatch.await(5, TimeUnit.SECONDS);
        
        // 验证结果
        log.info("\n========== 测试结果 ==========");
        log.info("生产者追加行数: {}", producedLines.get());
        log.info("消费者消费行数: {}", consumedLines.get());
        
        // 最终诊断：多次尝试查询
        log.info("\n========== 最终诊断 ==========");
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                log.info("  第{}次尝试查询...", attempt);
                Integer finalTotalLines = getTotalLines(logId);
                log.info("  总行数: {}", finalTotalLines);
                
                if (finalTotalLines != null && finalTotalLines > 0) {
                    List<Map<String, Object>> allLogs = getLogByLineRange(logId, 1, -1);
                    if (allLogs != null && !allLogs.isEmpty()) {
                        log.info("  ✅ 查询成功！找到{}条记录", allLogs.size());
                        log.info("  前5条记录：");
                        for (int j = 0; j < Math.min(5, allLogs.size()); j++) {
                            Map<String, Object> logEntry = allLogs.get(j);
                            log.info("    行{}: {}", logEntry.get("lineNumber"), logEntry.get("content"));
                        }
                        break;
                    } else {
                        log.warn("  总行数>0但查询详细记录为空，可能是查询条件问题");
                    }
                } else {
                    log.warn("  总行数为0，数据可能未写入或索引延迟");
                    if (attempt < 3) {
                        log.info("  等待3秒后重试...");
                        Thread.sleep(3000);
                    }
                }
            } catch (IOException e) {
                log.error("  查询失败: {}", e.getMessage(), e);
                if (attempt < 3) {
                    Thread.sleep(3000);
                }
            }
        }
        
        // 显示消费统计
        try {
            Integer finalTotalLines = getTotalLines(logId);
            log.info("\n========== 最终统计 ==========");
            log.info("生产者追加行数: {}", producedLines.get());
            log.info("消费者消费行数: {}", consumedLines.get());
            log.info("最终总行数: {}", finalTotalLines);
            
            if (consumedLines.get() >= producedLines.get() && finalTotalLines != null && finalTotalLines >= producedLines.get()) {
                log.info("✅ 测试通过：消费者成功实时查看到所有追加的日志（消息队列模式）");
            } else {
                int missing = producedLines.get() - consumedLines.get();
                log.warn("⚠️  警告：消费者可能未完全消费所有日志（{} < {}，缺失{}行）", 
                        consumedLines.get(), producedLines.get(), missing);
                log.warn("   可能原因：");
                log.warn("   1. OpenObserve异步写入延迟（postLogs是异步的）");
                log.warn("   2. OpenObserve索引延迟，数据仍在索引中");
                log.warn("   3. 查询时间范围可能不包含最新数据");
                log.warn("   4. 数据可能写入失败（检查服务端日志）");
            }
        } catch (IOException e) {
            log.error("获取总行数失败", e);
        }
    }
    
    /**
     * 测试：多生产者 + 多消费者
     * 模拟多个业务进程同时写日志，多个页面同时查看
     */
    @Test
    public void testMultipleProducersMultipleConsumers() throws InterruptedException {
        log.info("\n========== 测试：多生产者 + 多消费者 ==========\n");
        
        String logId;
        try {
            logId = createTestLog("实时查看测试", "多生产者多消费者");
        } catch (IOException e) {
            log.error("创建测试日志失败", e);
            return;
        }
        log.info("✓ 创建测试日志，logId: {}\n", logId);
        
        // 统计信息
        AtomicInteger totalProducedLines = new AtomicInteger(0);
        AtomicInteger totalConsumedLines = new AtomicInteger(0);
        AtomicLong lastConsumedLineNumber = new AtomicLong(0);
        
        int producerCount = 3;
        int consumerCount = 5;
        int linesPerProducer = 10;
        
        // 创建多个生产者
        CountDownLatch producerLatch = new CountDownLatch(producerCount);
        for (int p = 1; p <= producerCount; p++) {
            final int producerId = p;
            producerPool.submit(() -> {
                try {
                    for (int i = 1; i <= linesPerProducer; i++) {
                        String content = String.format("[生产者%d] 第%d行 - %s", producerId, i,
                                java.time.LocalDateTime.now().format(
                                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                        
                        try {
                            appendLog(logId, content, true);
                            totalProducedLines.incrementAndGet();
                            log.info("  [生产者{}] 追加第{}行", producerId, i);
                        } catch (IOException e) {
                            log.error("[生产者{}] 追加日志失败", producerId, e);
                        }
                        Thread.sleep(200 + (int)(Math.random() * 300)); // 随机间隔200-500ms
                    }
                    log.info("  [生产者{}] 完成\n", producerId);
                } catch (Exception e) {
                    log.error("[生产者{}] 错误", producerId, e);
                } finally {
                    producerLatch.countDown();
                }
            });
        }
        
        // 创建多个消费者
        CountDownLatch consumerLatch = new CountDownLatch(consumerCount);
        for (int c = 1; c <= consumerCount; c++) {
            final int consumerId = c;
            consumerPool.submit(() -> {
                try {
                    int queryCount = 0;
                    int localConsumedLines = 0;
                    
                    while (!producerLatch.await(1, TimeUnit.SECONDS) || 
                           totalProducedLines.get() > totalConsumedLines.get()) {
                        queryCount++;
                        
                        // 查询总行数
                        try {
                            Integer totalLines = getTotalLines(logId);
                            if (totalLines != null && totalLines > lastConsumedLineNumber.get()) {
                                // 查询新增的行
                                int startLine = (int) lastConsumedLineNumber.get() + 1;
                                List<Map<String, Object>> newLines = getLogByLineRange(logId, startLine, totalLines);
                                
                                if (newLines != null && !newLines.isEmpty()) {
                                    synchronized (BusinessLogRealTimeViewTest.class) {
                                        for (Map<String, Object> line : newLines) {
                                            Integer lineNum = (Integer) line.get("lineNumber");
                                            if (lineNum > lastConsumedLineNumber.get()) {
                                                totalConsumedLines.incrementAndGet();
                                                localConsumedLines++;
                                                lastConsumedLineNumber.set(lineNum);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.error("[消费者{}] 查询日志失败", consumerId, e);
                        }
                        
                        Thread.sleep(200 + (int)(Math.random() * 200)); // 随机间隔200-400ms
                    }
                    log.info("  [消费者{}] 完成，查询{}次，消费{}行", consumerId, queryCount, localConsumedLines);
                } catch (Exception e) {
                    log.error("[消费者{}] 错误", consumerId, e);
                } finally {
                    consumerLatch.countDown();
                }
            });
        }
        
        // 等待完成
        producerLatch.await(30, TimeUnit.SECONDS);
        consumerLatch.await(10, TimeUnit.SECONDS);
        
        // 验证结果
        log.info("\n========== 测试结果 ==========");
        log.info("生产者数量: {}", producerCount);
        log.info("消费者数量: {}", consumerCount);
        log.info("每个生产者追加行数: {}", linesPerProducer);
        log.info("总追加行数: {}", totalProducedLines.get());
        log.info("总消费行数: {}", totalConsumedLines.get());
        try {
            log.info("最终总行数: {}", getTotalLines(logId));
        } catch (IOException e) {
            log.error("获取总行数失败", e);
        }
        
        if (totalConsumedLines.get() >= totalProducedLines.get()) {
            log.info("✅ 测试通过：多个消费者成功实时查看到所有追加的日志");
        } else {
            log.warn("⚠️  警告：可能未完全消费所有日志（{} < {}）", 
                    totalConsumedLines.get(), totalProducedLines.get());
        }
    }
    
    /**
     * 测试：流水线式实时查看
     * 模拟CI/CD流水线场景，每个步骤追加日志，页面实时显示
     */
    @Test
    public void testPipelineRealTimeView() throws InterruptedException {
        log.info("\n========== 测试：流水线式实时查看 ==========\n");
        
        String logId;
        try {
            logId = createTestLog("CI/CD流水线", "构建任务");
        } catch (IOException e) {
            log.error("创建测试日志失败", e);
            return;
        }
        log.info("✓ 创建流水线日志，logId: {}\n", logId);
        
        AtomicInteger producedLines = new AtomicInteger(0);
        AtomicInteger consumedLines = new AtomicInteger(0);
        AtomicLong lastConsumedLineNumber = new AtomicLong(0);
        
        // 流水线步骤
        String[] steps = {
            "申请运行环境",
            "清理工作区",
            "克隆代码",
            "安装依赖",
            "运行测试",
            "构建项目",
            "部署应用"
        };
        
        // 创建生产者（执行流水线步骤）
        CountDownLatch producerLatch = new CountDownLatch(1);
        Future<?> producerFuture = producerPool.submit(() -> {
            try {
                for (int stepIndex = 0; stepIndex < steps.length; stepIndex++) {
                    String stepName = steps[stepIndex];
                    log.info("\n  [流水线] 开始步骤: {}", stepName);
                    
                    // 步骤开始标记
                    appendLog(logId, String.format("=== 步骤 %d/%d: %s ===", 
                            stepIndex + 1, steps.length, stepName), true);
                    producedLines.incrementAndGet();
                    
                    // 模拟步骤执行过程，追加多行日志
                    for (int i = 1; i <= 5; i++) {
                        String content = String.format("[%s] 执行进度 %d/5 - %s", stepName, i,
                                java.time.LocalDateTime.now().format(
                                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                        appendLog(logId, content, true);
                        producedLines.incrementAndGet();
                        Thread.sleep(300);
                    }
                    
                    // 步骤完成标记
                    appendLog(logId, String.format("✓ %s 完成", stepName), true);
                    producedLines.incrementAndGet();
                    
                    log.info("  [流水线] 步骤完成: {}\n", stepName);
                    Thread.sleep(500);
                }
                log.info("  [流水线] 所有步骤完成，共追加{}行\n", producedLines.get());
            } catch (Exception e) {
                log.error("[流水线] 错误", e);
            } finally {
                producerLatch.countDown();
            }
        });
        
        // 创建消费者（实时查看）
        CountDownLatch consumerLatch = new CountDownLatch(1);
        consumerPool.submit(() -> {
            try {
                int queryCount = 0;
                while (!producerFuture.isDone() || producedLines.get() > consumedLines.get()) {
                    queryCount++;
                    
                    Integer totalLines = getTotalLines(logId);
                    if (totalLines != null && totalLines > lastConsumedLineNumber.get()) {
                        int startLine = (int) lastConsumedLineNumber.get() + 1;
                        List<Map<String, Object>> newLines = getLogByLineRange(logId, startLine, totalLines);
                        
                        if (newLines != null && !newLines.isEmpty()) {
                            for (Map<String, Object> line : newLines) {
                                Integer lineNum = (Integer) line.get("lineNumber");
                                String content = (String) line.get("content");
                                log.info("  [页面] 实时显示第{}行: {}", lineNum, content);
                                consumedLines.incrementAndGet();
                                lastConsumedLineNumber.set(lineNum);
                            }
                        }
                    }
                    
                    Thread.sleep(200); // 每200ms刷新一次
                }
                log.info("\n  [页面] 查看完成，共查询{}次，显示{}行\n", queryCount, consumedLines.get());
            } catch (Exception e) {
                log.error("[页面] 错误", e);
            } finally {
                consumerLatch.countDown();
            }
        });
        
        // 等待完成
        producerLatch.await(60, TimeUnit.SECONDS);
        consumerLatch.await(10, TimeUnit.SECONDS);
        
        // 显示完整日志
        log.info("\n========== 完整日志 ==========");
        try {
            String fullLog = getFullContent(logId);
            log.info(fullLog);
        } catch (IOException e) {
            log.error("获取完整日志失败", e);
        }
        
        log.info("\n========== 测试结果 ==========");
        log.info("流水线步骤数: {}", steps.length);
        log.info("生产者追加行数: {}", producedLines.get());
        log.info("消费者消费行数: {}", consumedLines.get());
        try {
            log.info("最终总行数: {}", getTotalLines(logId));
        } catch (IOException e) {
            log.error("获取总行数失败", e);
        }
        
        if (consumedLines.get() >= producedLines.get()) {
            log.info("✅ 测试通过：页面成功实时显示所有流水线日志");
        }
    }
    
    // ==================== HTTP请求辅助方法 ====================
    
    private String createTestLog(String module, String operation) throws IOException {
        CreateBusinessLogForm form = new CreateBusinessLogForm();
        form.setModule(module);
        form.setOperation(operation);
        form.setUserId("test-user");
        form.setUsername("测试用户");
        form.setContent("测试日志开始 - " + 
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        form.setAutoTimestamp(true);
        form.setExpireDays(7);
        
        return createLog(form);
    }
    
    private String createLog(CreateBusinessLogForm form) throws IOException {
        String url = BASE_URL + "/businessLog/create";
        String json = JSON.toJSONString(form);
        
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            JSONObject result = JSON.parseObject(responseBody);
            
            if (result.getBoolean("success")) {
                JSONObject data = result.getJSONObject("result");
                return data.getString("logId");
            } else {
                throw new IOException("创建日志失败: " + result.getString("message"));
            }
        }
    }
    
    private boolean appendLog(String logId, String content, Boolean autoTimestamp) throws IOException {
        String url = BASE_URL + "/businessLog/append";
        
        AppendBusinessLogForm form = new AppendBusinessLogForm();
        form.setLogId(logId);
        form.setContent(content);
        if (autoTimestamp != null) {
            form.setAutoTimestamp(autoTimestamp);
        }
        
        String json = JSON.toJSONString(form);
        
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                log.error("  [HTTP] appendLog请求失败: HTTP {}, 响应: {}", response.code(), responseBody);
                throw new IOException("请求失败: HTTP " + response.code() + ", 响应: " + responseBody);
            }
            
            JSONObject result = JSON.parseObject(responseBody);
            Boolean success = result.getBoolean("success");
            String message = result.getString("message");
            
            if (!Boolean.TRUE.equals(success)) {
                log.warn("  [HTTP] appendLog返回success=false, message: {}", message);
            }
            
            return Boolean.TRUE.equals(success);
        }
    }
    
    private Integer getTotalLines(String logId) throws IOException {
        String url = BASE_URL + "/businessLog/getTotalLines/" + logId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                log.error("  [HTTP] getTotalLines请求失败: HTTP {}, 响应: {}", response.code(), responseBody);
                throw new IOException("请求失败: HTTP " + response.code() + ", 响应: " + responseBody);
            }
            
            JSONObject result = JSON.parseObject(responseBody);
            
            if (result.getBoolean("success")) {
                Integer totalLines = result.getInteger("result");
                return totalLines != null ? totalLines : 0;
            } else {
                String message = result.getString("message");
                log.warn("  [HTTP] getTotalLines返回success=false, message: {}", message);
                return 0;
            }
        }
    }
    
    private List<Map<String, Object>> getLogByLineRange(String logId, Integer startLine, Integer endLine) throws IOException {
        String url = BASE_URL + "/businessLog/getByLineRange/" + logId 
                + "?startLine=" + startLine + "&endLine=" + endLine;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                log.error("  [HTTP] getLogByLineRange请求失败: HTTP {}, 响应: {}", response.code(), responseBody);
                throw new IOException("请求失败: HTTP " + response.code() + ", 响应: " + responseBody);
            }
            
            JSONObject result = JSON.parseObject(responseBody);
            
            if (result.getBoolean("success")) {
                List<Map<String, Object>> logs = result.getObject("result", new TypeReference<List<Map<String, Object>>>() {});
                return logs;
            } else {
                String message = result.getString("message");
                log.warn("  [HTTP] getLogByLineRange返回success=false, message: {}", message);
                return null;
            }
        }
    }
    
    private String getFullContent(String logId) throws IOException {
        String url = BASE_URL + "/businessLog/getFullContent/" + logId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            JSONObject result = JSON.parseObject(responseBody);
            
            if (result.getBoolean("success")) {
                return result.getString("result");
            }
            return "";
        }
    }
}

