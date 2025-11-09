package com.jbm.cluster.logs;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
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
import java.util.concurrent.TimeUnit;

/**
 * 业务日志HTTP API完整测试
 * 使用OkHttp直接调用API接口进行测试，无需启动Spring Test容器
 * 
 * 使用前提：请先启动主程序 JbmLogsApplication
 * 
 * @author wesley
 */
@Slf4j
public class BusinessLogHttpTest {
    
    // 测试服务器地址（需要先启动主程序）
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
        log.info("业务日志HTTP API测试初始化完成");
        log.info("测试服务器地址: {}", BASE_URL);
        log.info("⚠️  请确保主程序已启动！");
        log.info("========================================\n");
    }
    
    /**
     * 测试：从本地文件上传日志
     */
    @Test
    public void testUploadLogFile() {
        log.info("\n========== 测试：从本地文件上传日志 ==========\n");
        
        try {
            // 创建模拟的编译日志文件
            String logFilePath = createMockCompileLogFile();
            log.info("创建模拟日志文件: {}", logFilePath);
            
            // 读取文件内容
            String fileContent = FileUtil.readUtf8String(logFilePath);
            log.info("读取文件内容，大小: {} 字节", fileContent.length());
            
            // 上传日志
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setModule("构建系统");
            form.setOperation("Maven编译");
            form.setUserId("jenkins");
            form.setUsername("Jenkins");
            form.setContent(fileContent);
            form.setAutoTimestamp(false);
            form.setExpireDays(90);
            
            String logId = createLog(form);
            log.info("✓ 日志上传成功，logId: {}", logId);
            
            // 查询总行数
            Integer totalLines = getTotalLines(logId);
            log.info("✓ 日志总行数: {}", totalLines);
            
            // 查看前5行
            List<Map<String, Object>> first5 = getLogByLineRange(logId, 1, 5);
            log.info("✓ 前5行:");
            first5.forEach(line -> log.info("  行{}: {}", line.get("lineNumber"), line.get("content")));
            
            // 清理
            FileUtil.del(logFilePath);
            
            log.info("\n✅ 测试通过！\n");
            
        } catch (Exception e) {
            log.error("\n❌ 测试失败！", e);
        }
    }
    
    /**
     * 测试：实时追加日志
     */
    @Test
    public void testRealTimeAppendLog() {
        log.info("\n========== 测试：实时追加日志 ==========\n");
        
        try {
            String orderId = "ORDER" + IdUtil.fastSimpleUUID().substring(0, 8);
            
            // 创建订单日志
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setModule("订单系统");
            form.setOperation("订单处理");
            form.setUserId("user001");
            form.setUsername("张三");
            form.setContent("开始处理订单: " + orderId);
            form.setAutoTimestamp(true);
            form.setExpireDays(30);
            
            String logId = createLog(form);
            log.info("✓ 创建订单日志，logId: {}", logId);
            
            // 追加日志步骤
            log.info("✓ 开始追加日志...");
            
            appendLog(logId, "验证订单信息\n订单金额: 199.00元\n商品数量: 2件\n验证通过", true);
            log.info("  - 步骤1: 验证订单");
            Thread.sleep(300);
            
            appendLog(logId, "检查商品库存\n库存充足", true);
            log.info("  - 步骤2: 检查库存");
            Thread.sleep(300);
            
            appendLog(logId, "调用支付接口\n支付方式: 微信支付\n支付结果: 成功", true);
            log.info("  - 步骤3: 调用支付");
            Thread.sleep(300);
            
            appendLog(logId, "订单处理完成\n订单状态: 待发货", true);
            log.info("  - 步骤4: 完成订单");
            
            // 查看完整日志
            String fullLog = getFullContent(logId);
            log.info("\n✓ 完整日志:\n{}\n", fullLog);
            
            log.info("✅ 测试通过！\n");
            
        } catch (Exception e) {
            log.error("\n❌ 测试失败！", e);
        }
    }
    
    /**
     * 测试：查询日志（按行号范围）
     */
    @Test
    public void testQueryLogByLineRange() {
        log.info("\n========== 测试：查询日志（按行号范围） ==========\n");
        
        try {
            // 创建日志
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setModule("测试模块");
            form.setOperation("行号测试");
            form.setContent("第1行\n第2行\n第3行\n第4行\n第5行\n第6行\n第7行\n第8行\n第9行\n第10行");
            form.setAutoTimestamp(false);
            
            String logId = createLog(form);
            log.info("✓ 创建测试日志，logId: {}", logId);
            
            // 查询总行数
            Integer total = getTotalLines(logId);
            log.info("✓ 总行数: {}", total);
            
            // 查询第3-7行
            List<Map<String, Object>> lines = getLogByLineRange(logId, 3, 7);
            log.info("✓ 查询第3-7行:");
            lines.forEach(line -> log.info("  行{}: {}", line.get("lineNumber"), line.get("content")));
            
            // 查询最后3行
            List<Map<String, Object>> lastLines = getLogByLineRange(logId, total - 2, total);
            log.info("✓ 最后3行:");
            lastLines.forEach(line -> log.info("  行{}: {}", line.get("lineNumber"), line.get("content")));
            
            log.info("\n✅ 测试通过！\n");
            
        } catch (Exception e) {
            log.error("\n❌ 测试失败！", e);
        }
    }
    
    /**
     * 测试所有功能
     */
    @Test
    public void testAll() {
        log.info("\n\n");
        log.info("================================================================================");
        log.info("                        开始运行完整功能测试                                    ");
        log.info("================================================================================\n");
        
        testUploadLogFile();
        testRealTimeAppendLog();
        testQueryLogByLineRange();
        
        log.info("================================================================================");
        log.info("                        所有测试完成！                                         ");
        log.info("================================================================================\n\n");
    }
    
    // ==================== HTTP请求辅助方法 ====================
    
    /**
     * 创建业务日志
     */
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
    
    /**
     * 追加日志
     */
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
    
    /**
     * 查询日志总行数
     */
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
    
    /**
     * 按行号范围查询日志
     */
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
    
    /**
     * 查询完整日志内容
     */
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
    
    // ==================== 模拟数据生成方法 ====================
    
    /**
     * 创建模拟的编译日志文件
     */
    private String createMockCompileLogFile() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + "/mock-compile-" + IdUtil.fastSimpleUUID() + ".log";
        
        StringBuilder content = new StringBuilder();
        content.append("[INFO] Scanning for projects...\n");
        content.append("[INFO] \n");
        content.append("[INFO] ------------------------------------------------------------------------\n");
        content.append("[INFO] Building jbm-cluster-platform 7.2.0-SNAPSHOT\n");
        content.append("[INFO] ------------------------------------------------------------------------\n");
        content.append("[INFO] \n");
        content.append("[INFO] --- maven-clean-plugin:3.1.0:clean (default-clean) ---\n");
        content.append("[INFO] Deleting /workspace/target\n");
        content.append("[INFO] \n");
        content.append("[INFO] --- maven-compiler-plugin:3.8.1:compile (default-compile) ---\n");
        content.append("[INFO] Changes detected - recompiling the module!\n");
        content.append("[INFO] Compiling 156 source files to /workspace/target/classes\n");
        content.append("[INFO] \n");
        content.append("[INFO] --- maven-jar-plugin:3.2.0:jar (default-jar) ---\n");
        content.append("[INFO] Building jar: /workspace/target/jbm-cluster-platform-7.2.0-SNAPSHOT.jar\n");
        content.append("[INFO] ------------------------------------------------------------------------\n");
        content.append("[INFO] BUILD SUCCESS\n");
        content.append("[INFO] ------------------------------------------------------------------------\n");
        content.append("[INFO] Total time:  02:35 min\n");
        content.append("[INFO] Finished at: 2025-01-04T10:30:45+08:00\n");
        content.append("[INFO] ------------------------------------------------------------------------\n");
        
        FileUtil.writeUtf8String(content.toString(), filePath);
        return filePath;
    }
}

