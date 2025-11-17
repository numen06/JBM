package com.jbm.cluster.logs;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 业务日志流水线测试
 * 模拟CI/CD流水线样式的分步骤日志记录
 * 
 * 类似界面功能：
 * - 左侧：步骤列表（申请运行环境、清理工作区、克隆代码等）
 * - 右侧：当前步骤的实时日志输出
 * - 支持查看完整日志、下载日志
 * - 每个步骤显示执行时间
 * 
 * @author wesley
 */
@Slf4j
public class BusinessLogPipelineTest {
    
    // 测试服务器地址
    private static final String BASE_URL = "http://localhost:3312";
    
    // HTTP客户端
    private static OkHttpClient httpClient;
    
    @BeforeAll
    public static void setup() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        log.info("\n========================================");
        log.info("业务日志流水线测试");
        log.info("模拟CI/CD流水线样式的日志记录");
        log.info("========================================\n");
    }
    
    /**
     * 测试：模拟构建流水线日志
     * 类似：申请运行环境 → 清理工作区 → 克隆代码 → 构建 → 上传缓存
     */
    @Test
    public void testBuildPipeline() {
        log.info("\n========== 测试：构建流水线日志 ==========\n");
        
        try {
            String pipelineId = "PIPELINE-" + IdUtil.fastSimpleUUID().substring(0, 8);
            
            // 创建流水线日志
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setModule("CI/CD流水线");
            form.setOperation("构建流水线");
            form.setUserId("jenkins");
            form.setUsername("Jenkins构建机器人");
            form.setContent(String.format(
                "[流水线开始] Pipeline ID: %s\n" +
                "[触发信息] 由 numen06@example.com 触发\n" +
                "[流水线源] feige-manage/master/2e613428\n" +
                "[提交信息] 修复内容",
                pipelineId
            ));
            form.setAutoTimestamp(true);
            form.setExpireDays(90);
            
            String logId = createLog(form);
            log.info("✓ 创建流水线日志，logId: {}", logId);
            log.info("✓ Pipeline ID: {}\n", pipelineId);
            
            // 步骤1：申请运行环境
            long step1Start = System.currentTimeMillis();
            logStep(logId, "申请运行环境", () -> {
                appendLogLine(logId, "[executionStep begins at " + getCurrentTime() + "]");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 开始申请运行环境...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 检查可用资源...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 分配容器: container-001");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 运行环境准备完成");
                appendLogLine(logId, "[" + getCurrentTime() + "] [SUCCESS] 申请运行环境成功");
            });
            long step1Duration = (System.currentTimeMillis() - step1Start) / 1000;
            log.info("  ✓ 步骤1完成，耗时: {}s\n", step1Duration);
            Thread.sleep(500);
            
            // 步骤2：清理工作区
            long step2Start = System.currentTimeMillis();
            logStep(logId, "清理工作区", () -> {
                appendLogLine(logId, "[executionStep begins at " + getCurrentTime() + "]");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 开始清理工作区...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 删除旧文件...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 清理缓存...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 清理完成!");
                appendLogLine(logId, "[" + getCurrentTime() + "] [SUCCESS] 执行命令成功");
            });
            long step2Duration = (System.currentTimeMillis() - step2Start) / 1000;
            log.info("  ✓ 步骤2完成，耗时: {}s\n", step2Duration);
            Thread.sleep(500);
            
            // 步骤3：克隆代码
            long step3Start = System.currentTimeMillis();
            logStep(logId, "克隆代码", () -> {
                appendLogLine(logId, "[executionStep begins at " + getCurrentTime() + "]");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 开始克隆代码...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 仓库地址: git@github.com:example/feige-manage.git");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 分支: master");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 提交: 2e613428");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] Cloning into 'workspace'...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] Receiving objects: 100% (156/156), done.");
                appendLogLine(logId, "[" + getCurrentTime() + "] [SUCCESS] 代码克隆成功");
            });
            long step3Duration = (System.currentTimeMillis() - step3Start) / 1000;
            log.info("  ✓ 步骤3完成，耗时: {}s\n", step3Duration);
            Thread.sleep(500);
            
            // 步骤4：流水线缓存
            long step4Start = System.currentTimeMillis();
            logStep(logId, "流水线缓存", () -> {
                appendLogLine(logId, "[executionStep begins at " + getCurrentTime() + "]");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 开始加载缓存...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 检查缓存键: pipeline-cache-v1");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 缓存命中，开始解压...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 解压完成: node_modules/");
                appendLogLine(logId, "[" + getCurrentTime() + "] [SUCCESS] 缓存加载成功");
            });
            long step4Duration = (System.currentTimeMillis() - step4Start) / 1000;
            log.info("  ✓ 步骤4完成，耗时: {}s\n", step4Duration);
            Thread.sleep(500);
            
            // 步骤5：构建任务（feige-manager）
            long step5Start = System.currentTimeMillis();
            logStep(logId, "feige-manager", () -> {
                appendLogLine(logId, "[executionStep begins at " + getCurrentTime() + "]");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 开始构建 feige-manager...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 运行命令: npm install");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] npm WARN deprecated package@1.0.0");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] added 1234 packages in 25s");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 运行命令: npm run build");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] > feige-manager@1.0.0 build");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] > webpack --mode production");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] webpack compiled successfully");
                appendLogLine(logId, "[" + getCurrentTime() + "] [SUCCESS] 构建完成");
            });
            long step5Duration = (System.currentTimeMillis() - step5Start) / 1000;
            log.info("  ✓ 步骤5完成，耗时: {}s\n", step5Duration);
            Thread.sleep(500);
            
            // 步骤6：缓存上传
            long step6Start = System.currentTimeMillis();
            logStep(logId, "缓存上传", () -> {
                appendLogLine(logId, "[executionStep begins at " + getCurrentTime() + "]");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 开始上传缓存...");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 压缩文件: node_modules/");
                appendLogLine(logId, "[" + getCurrentTime() + "] [INFO] 上传到: s3://cache-bucket/pipeline-cache-v1.tar.gz");
                appendLogLine(logId, "[" + getCurrentTime() + "] [SUCCESS] 缓存上传成功");
            });
            long step6Duration = (System.currentTimeMillis() - step6Start) / 1000;
            log.info("  ✓ 步骤6完成，耗时: {}s\n", step6Duration);
            
            // 显示步骤摘要
            log.info("\n========== 流水线步骤摘要 ==========");
            log.info("步骤1: 申请运行环境 ({}s) ✓", step1Duration);
            log.info("步骤2: 清理工作区 ({}s) ✓", step2Duration);
            log.info("步骤3: 克隆代码 ({}s) ✓", step3Duration);
            log.info("步骤4: 流水线缓存 ({}s) ✓", step4Duration);
            log.info("步骤5: feige-manager ({}s) ✓", step5Duration);
            log.info("步骤6: 缓存上传 ({}s) ✓", step6Duration);
            log.info("总耗时: {}s", step1Duration + step2Duration + step3Duration + step4Duration + step5Duration + step6Duration);
            
            // 查看完整日志
            String fullLog = getFullContent(logId);
            log.info("\n========== 完整日志（前500字符） ==========");
            log.info(fullLog.substring(0, Math.min(500, fullLog.length())));
            log.info("...\n");
            
            // 模拟查看某个步骤的日志（类似界面点击步骤）
            log.info("========== 查看步骤2的日志 ==========");
            // 假设步骤2的日志在第10-15行（实际需要根据内容定位）
            List<Map<String, Object>> step2Logs = getLogByLineRange(logId, 10, 15);
            if (step2Logs != null) {
                step2Logs.forEach(line -> log.info("  {}", line.get("content")));
            }
            
            log.info("\n✅ 流水线测试完成！\n");
            log.info("logId: {}", logId);
            log.info("可以通过以下方式查看日志：");
            log.info("  - 完整日志: GET {}/businessLog/getFullContent/{}", BASE_URL, logId);
            log.info("  - 按行查询: GET {}/businessLog/getByLineRange/{}?startLine=1&endLine=100", BASE_URL, logId);
            
        } catch (Exception e) {
            log.error("\n❌ 测试失败！", e);
        }
    }
    
    /**
     * 记录一个步骤的日志
     */
    private void logStep(String logId, String stepName, StepAction stepAction) throws IOException {
        // 添加步骤分隔符
        String separator = StrUtil.repeat("=", 80);
        appendLogLine(logId, "\n" + separator);
        appendLogLine(logId, String.format("[步骤开始] %s", stepName));
        appendLogLine(logId, separator);
        
        // 执行步骤
        stepAction.execute();
        
        // 步骤结束标记
        appendLogLine(logId, String.format("[步骤完成] %s", stepName));
    }
    
    /**
     * 步骤执行接口
     */
    @FunctionalInterface
    private interface StepAction {
        void execute() throws IOException;
    }
    
    /**
     * 追加一行日志
     */
    private void appendLogLine(String logId, String line) throws IOException {
        appendLog(logId, line, false);
    }
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }
    
    // ==================== HTTP请求辅助方法 ====================
    
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
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            JSONObject result = JSON.parseObject(responseBody);
            
            return result.getBoolean("success");
        }
    }
    
    private Integer getTotalLines(String logId) throws IOException {
        String url = BASE_URL + "/businessLog/getTotalLines/" + logId;
        
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
                return result.getInteger("result");
            }
            return 0;
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
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            JSONObject result = JSON.parseObject(responseBody);
            
            if (result.getBoolean("success")) {
                return result.getObject("result", new TypeReference<List<Map<String, Object>>>() {});
            }
            return null;
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

    @Test
    public void testFullContent() throws IOException {
        String logId = "cac6976ff9d44729b75f6e5cfe61b96e";
        String fullContent = getFullContent(logId);
        log.info("完整日志: {}", fullContent);
    }


}

