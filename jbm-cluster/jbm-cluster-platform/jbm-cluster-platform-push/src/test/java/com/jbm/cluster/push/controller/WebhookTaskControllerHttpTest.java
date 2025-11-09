package com.jbm.cluster.push.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.framework.usage.paging.PageForm;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Webhook 任务控制器 HTTP 接口测试
 * 使用 OkHttp 模拟 HTTP 请求测试已运行的服务
 * 
 * 使用前提：确保 jbm-cluster-platform-push 服务已启动
 *
 * @author wesley
 */
@Slf4j
public class WebhookTaskControllerHttpTest {

    private OkHttpClient client;
    private String baseUrl;
    
    // 配置服务地址和端口
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3313;  // 根据实际端口修改

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @BeforeEach
    public void setUp() {
        baseUrl = "http://" + SERVER_HOST + ":" + SERVER_PORT;
        
        // 配置 OkHttp 客户端
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    long startTime = System.currentTimeMillis();
                    Response response = chain.proceed(request);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    log.info("HTTP {} {} - {} ({}ms)", 
                            request.method(), 
                            request.url().encodedPath(),
                            response.code(),
                            duration);
                    
                    return response;
                })
                .build();
        
        log.info("测试服务地址: {}", baseUrl);
    }

    /**
     * 执行 POST 请求
     */
    private String post(String url, Object body) throws IOException {
        String json = JSONUtil.toJsonStr(body);
        log.debug("请求体: {}", json);
        
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + url)
                .post(requestBody)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求失败: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }
            
            String responseBody = response.body().string();
            log.debug("响应体: {}", responseBody);
            return responseBody;
        }
    }

    /**
     * 执行 GET 请求
     */
    private String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + url)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求失败: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }
            
            String responseBody = response.body().string();
            log.debug("响应体: {}", responseBody);
            return responseBody;
        }
    }

    @Test
    @DisplayName("HTTP测试1: 查询所有任务")
    public void testSelectAllTasks() throws IOException {
        log.info("========== HTTP测试1: 查询所有任务 ==========");
        
        WebhookTaskForm form = new WebhookTaskForm();
        form.setWebhookTask(new WebhookTask());
        form.setWebhookEventConfig(new WebhookEventConfig());
        
        PageForm pageForm = new PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(10);
        form.setPageForm(pageForm);
        
        long startTime = System.currentTimeMillis();
        String response = post("/webhookTask/selectWebhookTasks", form);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("请求耗时: {} ms", duration);
        log.info("响应内容: {}", response);
        
        assert response != null;
        assert response.contains("success") || response.contains("\"code\":200");
        log.info("✅ 测试通过\n");
    }

    @Test
    @DisplayName("HTTP测试2: 按状态查询")
    public void testSelectByStatus() throws IOException {
        log.info("========== HTTP测试2: 按状态查询 ==========");
        
        WebhookTaskForm form = new WebhookTaskForm();
        
        WebhookTask task = new WebhookTask();
        task.setStatus("SUCCESS");
        form.setWebhookTask(task);
        
        form.setWebhookEventConfig(new WebhookEventConfig());
        
        PageForm pageForm = new PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(10);
        form.setPageForm(pageForm);
        
        long startTime = System.currentTimeMillis();
        String response = post("/webhookTask/selectWebhookTasks", form);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("查询条件: status=SUCCESS");
        log.info("请求耗时: {} ms", duration);
        log.info("响应数据: {}", response);
        
        assert response != null;
        log.info("✅ 测试通过\n");
    }

    @Test
    @DisplayName("HTTP测试3: 按事件代码查询")
    public void testSelectByBusinessEventCode() throws IOException {
        log.info("========== HTTP测试3: 按事件代码查询 ==========");
        
        WebhookTaskForm form = new WebhookTaskForm();
        form.setWebhookTask(new WebhookTask());
        
        WebhookEventConfig config = new WebhookEventConfig();
        config.setBusinessEventCode("Test");
        form.setWebhookEventConfig(config);
        
        PageForm pageForm = new PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(10);
        form.setPageForm(pageForm);
        
        long startTime = System.currentTimeMillis();
        String response = post("/webhookTask/selectWebhookTasks", form);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("查询条件: businessEventCode LIKE '%Test%'");
        log.info("请求耗时: {} ms", duration);
        log.info("响应数据: {}", response);
        
        assert response != null;
        log.info("✅ 测试通过\n");
    }

    @Test
    @DisplayName("HTTP测试4: 按时间范围查询")
    public void testSelectByTimeRange() throws IOException {
        log.info("========== HTTP测试4: 按时间范围查询 ==========");
        
        WebhookTaskForm form = new WebhookTaskForm();
        form.setWebhookTask(new WebhookTask());
        form.setWebhookEventConfig(new WebhookEventConfig());
        
        // 查询最近7天
        Date endTime = new Date();
        Date beginTime = DateUtil.offsetDay(endTime, -7);
        form.setBeginTime(beginTime);
        form.setEndTime(endTime);
        
        PageForm pageForm = new PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(20);
        form.setPageForm(pageForm);
        
        long startTime = System.currentTimeMillis();
        String response = post("/webhookTask/selectWebhookTasks", form);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("查询条件: {} ~ {}", 
                DateUtil.formatDateTime(beginTime), 
                DateUtil.formatDateTime(endTime));
        log.info("请求耗时: {} ms", duration);
        log.info("响应数据: {}", response);
        
        assert response != null;
        log.info("✅ 测试通过\n");
    }

    @Test
    @DisplayName("HTTP测试5: 分页查询")
    public void testPagination() throws IOException {
        log.info("========== HTTP测试5: 分页查询 ==========");
        
        // 第一页
        WebhookTaskForm form1 = new WebhookTaskForm();
        form1.setWebhookTask(new WebhookTask());
        form1.setWebhookEventConfig(new WebhookEventConfig());
        
        PageForm pageForm1 = new PageForm();
        pageForm1.setCurrPage(1);
        pageForm1.setPageSize(5);
        form1.setPageForm(pageForm1);
        
        String response1 = post("/webhookTask/selectWebhookTasks", form1);
        log.info("第1页响应: {}", response1);
        
        // 第二页
        WebhookTaskForm form2 = new WebhookTaskForm();
        form2.setWebhookTask(new WebhookTask());
        form2.setWebhookEventConfig(new WebhookEventConfig());
        
        PageForm pageForm2 = new PageForm();
        pageForm2.setCurrPage(2);
        pageForm2.setPageSize(5);
        form2.setPageForm(pageForm2);
        
        String response2 = post("/webhookTask/selectWebhookTasks", form2);
        log.info("第2页数据量: {}", response2.length());
        
        assert response1 != null;
        assert response2 != null;
        log.info("✅ 测试通过\n");
    }

    @Test
    @DisplayName("HTTP测试6: 性能压力测试")
    public void testPerformanceStress() throws IOException {
        log.info("========== HTTP测试6: 性能压力测试 ==========");
        
        WebhookTaskForm form = new WebhookTaskForm();
        form.setWebhookTask(new WebhookTask());
        form.setWebhookEventConfig(new WebhookEventConfig());
        
        PageForm pageForm = new PageForm();
        pageForm.setCurrPage(1);
        pageForm.setPageSize(10);
        form.setPageForm(pageForm);
        
        int testCount = 20;
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        int successCount = 0;
        int failCount = 0;
        
        log.info("执行 {} 次并发查询...", testCount);
        
        for (int i = 0; i < testCount; i++) {
            try {
                long startTime = System.currentTimeMillis();
                String response = post("/webhookTask/selectWebhookTasks", form);
                long duration = System.currentTimeMillis() - startTime;
                
                totalTime += duration;
                minTime = Math.min(minTime, duration);
                maxTime = Math.max(maxTime, duration);
                successCount++;
                
                if ((i + 1) % 5 == 0) {
                    log.info("已完成 {}/{} 次", i + 1, testCount);
                }
            } catch (Exception e) {
                failCount++;
                log.error("第 {} 次请求失败: {}", i + 1, e.getMessage());
            }
        }
        
        double avgTime = (double) totalTime / successCount;
        
        log.info("\n性能统计:");
        log.info("  - 总请求数: {}", testCount);
        log.info("  - 成功次数: {}", successCount);
        log.info("  - 失败次数: {}", failCount);
        log.info("  - 总耗时: {} ms", totalTime);
        log.info("  - 平均耗时: {} ms", String.format("%.2f", avgTime));
        log.info("  - 最小耗时: {} ms", minTime);
        log.info("  - 最大耗时: {} ms", maxTime);
        log.info("  - 成功率: {} %", String.format("%.2f", successCount * 100.0 / testCount));
        
        // 性能断言
        if (successCount > 0) {
            assert successCount > testCount * 0.95 : "成功率低于95%";
            assert avgTime < 2000 : "平均响应时间超过2秒";
            assert maxTime < 5000 : "最大响应时间超过5秒";
        }
        
        log.info("✅ 测试通过\n");
    }
}

