package com.jbm.cluster.ai;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 用户对话模拟测试
 * 通过真实的 HTTP 接口测试 AI 系统的对话功能
 * @author wesley
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserChatSimulationTest {

    private static final String BASE_URL = "http://localhost:3315";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private static OkHttpClient httpClient;
    private static String sessionId;
    
    @BeforeAll
    public static void setupClass() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("🤖 JBM AI 对话系统测试（HTTP 接口）");
        log.info("═══════════════════════════════════════════════════════");
        log.info("🌐 测试地址: {}", BASE_URL);
        
        // 初始化 OkHttp 客户端
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        
        // 检查服务是否可用
        try {
            Request healthCheck = new Request.Builder()
                    .url(BASE_URL + "/ai/health")
                    .get()
                    .build();
            
            Response response = httpClient.newCall(healthCheck).execute();
            if (response.isSuccessful()) {
                log.info("✅ 服务运行正常");
                log.info("⏳ 等待 10 秒让 API 元数据收集完成...");
                Thread.sleep(10000);
            } else {
                log.error("❌ 服务健康检查失败: HTTP {}", response.code());
            }
            response.close();
        } catch (Exception e) {
            log.error("❌ 无法连接到服务: {}", e.getMessage());
            log.error("请确保:");
            log.error("  1. 服务已启动在端口 3315");
            log.error("  2. 已设置 DASHSCOPE_API_KEY 环境变量");
        }
        
        log.info("═══════════════════════════════════════════════════════\n");
    }
    
    @BeforeEach
    public void setup() {
        // 每个测试间隔 2 秒
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    /**
     * 测试 1: 简单问候对话
     */
    @Test
    @Order(1)
    @DisplayName("场景1: 简单问候 - 建立初次联系")
    public void testScenario1_SimpleGreeting() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景1: 简单问候");
        log.info("═══════════════════════════════════════════════════════");
        
        // 用户第一次对话
        simulateUserChat("你好，我是新用户，你能做什么？", false);
        
        log.info("✅ 场景1完成：建立初次联系\n");
    }
    
    /**
     * 测试 2: 了解系统能力
     */
    @Test
    @Order(2)
    @DisplayName("场景2: 了解系统能力")
    public void testScenario2_UnderstandCapabilities() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景2: 了解系统能力");
        log.info("═══════════════════════════════════════════════════════");
        
        // 询问系统有哪些功能
        simulateUserChat("你可以帮我查询哪些信息？", false);
        
        log.info("✅ 场景2完成：了解系统能力\n");
    }
    
    /**
     * 测试 3: 查询系统状态（Function Calling）
     */
    @Test
    @Order(3)
    @DisplayName("场景3: 查询系统健康状态 - 首次 Function Calling")
    public void testScenario3_QuerySystemHealth() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景3: 查询系统健康状态");
        log.info("═══════════════════════════════════════════════════════");
        
        // 用户询问系统状态，AI 应该调用健康检查接口
        JSONObject response = simulateUserChat("帮我看一下系统现在的健康状态", true);
        
        if (response.getBool("functionCalled", false)) {
            log.info("✅ AI 成功调用了函数: {}", response.getStr("functionName"));
            log.info("📊 函数返回结果: {}", response.getStr("functionResult"));
        } else {
            log.info("ℹ️ AI 未调用函数（可能系统中没有相关接口）");
        }
        
        log.info("✅ 场景3完成：系统状态查询\n");
    }
    
    /**
     * 测试 4: 查询用户信息
     */
    @Test
    @Order(4)
    @DisplayName("场景4: 查询用户列表 - 数据查询场景")
    public void testScenario4_QueryUsers() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景4: 查询用户信息");
        log.info("═══════════════════════════════════════════════════════");
        
        // 用户想查看用户列表
        JSONObject response = simulateUserChat("我想看一下系统中的用户列表", true);
        
        if (response.getBool("functionCalled", false)) {
            log.info("✅ AI 成功理解意图并调用了函数: {}", response.getStr("functionName"));
        }
        
        log.info("✅ 场景4完成：用户信息查询\n");
    }
    
    /**
     * 测试 5: 多轮对话 - 上下文理解
     */
    @Test
    @Order(5)
    @DisplayName("场景5: 多轮对话 - 测试上下文理解")
    public void testScenario5_MultiTurnConversation() throws IOException, InterruptedException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景5: 多轮对话 - 上下文理解");
        log.info("═══════════════════════════════════════════════════════");
        
        // 第一轮：查询数据
        log.info("👤 用户（第1轮）: 查询最近的订单");
        JSONObject response1 = sendChatRequest("查询最近的订单", true, null);
        sessionId = response1.getStr("sessionId");
        log.info("🤖 AI（第1轮）: {}", response1.getStr("message"));
        
        if (response1.getBool("functionCalled", false)) {
            log.info("📞 调用函数: {}", response1.getStr("functionName"));
        }
        
        // 第二轮：追加条件（测试上下文）
        Thread.sleep(2000);
        
        log.info("\n👤 用户（第2轮）: 只要今天的");
        log.info("   （上下文：继续讨论订单查询）");
        JSONObject response2 = sendChatRequest("只要今天的", true, sessionId);
        log.info("🤖 AI（第2轮）: {}", response2.getStr("message"));
        
        if (response2.getBool("functionCalled", false)) {
            log.info("📞 调用函数: {}", response2.getStr("functionName"));
        }
        
        log.info("✅ 场景5完成：多轮对话测试\n");
    }
    
    /**
     * 测试 6: 复杂查询
     */
    @Test
    @Order(6)
    @DisplayName("场景6: 复杂查询 - 带参数的接口调用")
    public void testScenario6_ComplexQuery() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景6: 复杂查询 - 带参数");
        log.info("═══════════════════════════════════════════════════════");
        
        // 用户提出带参数的查询
        JSONObject response = simulateUserChat("帮我查询用户ID为1001的详细信息", true);
        
        if (response.getBool("functionCalled", false)) {
            log.info("✅ AI 成功提取参数并调用函数");
            log.info("📞 函数: {}", response.getStr("functionName"));
            log.info("📊 结果: {}", response.getStr("functionResult"));
        }
        
        log.info("✅ 场景6完成：复杂查询\n");
    }
    
    /**
     * 测试 7: 错误处理
     */
    @Test
    @Order(7)
    @DisplayName("场景7: 错误处理 - 无法找到合适的接口")
    public void testScenario7_ErrorHandling() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景7: 错误处理");
        log.info("═══════════════════════════════════════════════════════");
        
        // 用户询问一个系统无法处理的请求
        simulateUserChat("帮我订一张明天去北京的机票", true);
        
        log.info("✅ 场景7完成：错误处理测试\n");
    }
    
    /**
     * 测试 8: 清除会话
     */
    @Test
    @Order(8)
    @DisplayName("场景8: 清除会话 - 重新开始对话")
    public void testScenario8_ClearSession() throws IOException {
        if (sessionId == null) {
            log.warn("⚠️ 没有活跃的会话，跳过测试");
            return;
        }
        
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景8: 清除会话");
        log.info("═══════════════════════════════════════════════════════");
        
        log.info("🗑️ 清除会话: {}", sessionId);
        clearSession(sessionId);
        
        // 清除后重新对话
        simulateUserChat("你好", false);
        
        log.info("✅ 场景8完成：会话清除\n");
    }
    
    /**
     * 测试 9: 综合场景 - 完整业务流程
     */
    @Test
    @Order(9)
    @DisplayName("场景9: 综合业务流程")
    public void testScenario9_CompleteBusinessFlow() throws IOException, InterruptedException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景9: 综合业务流程");
        log.info("═══════════════════════════════════════════════════════");
        
        log.info("模拟用户完成一个完整的业务流程：");
        log.info("1. 查询信息");
        log.info("2. 分析数据");
        log.info("3. 总结结果\n");
        
        // 步骤1：查询信息
        log.info("👤 步骤1 - 查询订单: 查询最近一周的订单统计");
        JSONObject response1 = sendChatRequest("查询最近一周的订单统计", true, null);
        String flowSessionId = response1.getStr("sessionId");
        log.info("🤖 AI响应: {}", response1.getStr("message"));
        
        // 步骤2：追加查询
        Thread.sleep(2000);
        
        log.info("\n👤 步骤2 - 追加查询: 其中有多少是已完成的？");
        JSONObject response2 = sendChatRequest("其中有多少是已完成的？", true, flowSessionId);
        log.info("🤖 AI响应: {}", response2.getStr("message"));
        
        // 步骤3：总结
        Thread.sleep(2000);
        
        log.info("\n👤 步骤3 - 请求总结: 帮我总结一下整体情况");
        JSONObject response3 = sendChatRequest("帮我总结一下整体情况", false, flowSessionId);
        log.info("🤖 AI响应: {}", response3.getStr("message"));
        
        log.info("\n✅ 场景9完成：综合业务流程\n");
    }
    
    /**
     * 测试 10: 交互式对话（可选）
     */
    @Test
    @Order(10)
    @DisplayName("场景10: 交互式对话（手动测试）")
    @Disabled("手动测试时启用")
    public void testScenario10_InteractiveChat() throws IOException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景10: 交互式对话");
        log.info("═══════════════════════════════════════════════════════");
        log.info("输入 'exit' 或 'quit' 退出对话");
        log.info("输入 'clear' 清除会话");
        log.info("═══════════════════════════════════════════════════════\n");
        
        Scanner scanner = new Scanner(System.in);
        String interactiveSessionId = null;
        
        while (true) {
            System.out.print("\n👤 您: ");
            String userInput = scanner.nextLine().trim();
            
            if (userInput.equalsIgnoreCase("exit") || 
                userInput.equalsIgnoreCase("quit")) {
                log.info("👋 再见！");
                break;
            }
            
            if (userInput.equalsIgnoreCase("clear")) {
                if (interactiveSessionId != null) {
                    clearSession(interactiveSessionId);
                    log.info("🗑️ 会话已清除");
                }
                interactiveSessionId = null;
                continue;
            }
            
            if (userInput.isEmpty()) {
                continue;
            }
            
            try {
                JSONObject response = sendChatRequest(userInput, true, interactiveSessionId);
                interactiveSessionId = response.getStr("sessionId");
                
                System.out.println("🤖 AI: " + response.getStr("message"));
                
                if (response.getBool("functionCalled", false)) {
                    System.out.println("   📞 调用了函数: " + response.getStr("functionName"));
                }
                
                String error = response.getStr("error");
                if (error != null && !error.isEmpty()) {
                    System.out.println("   ⚠️ 错误: " + error);
                }
            } catch (Exception e) {
                System.out.println("   ❌ 请求失败: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    /**
     * 辅助方法：模拟用户对话
     */
    private JSONObject simulateUserChat(String message, boolean enableFunctions) throws IOException {
        log.info("👤 用户: {}", message);
        log.info("⚙️ Function Calling: {}", enableFunctions ? "启用" : "禁用");
        
        JSONObject response = sendChatRequest(message, enableFunctions, sessionId);
        sessionId = response.getStr("sessionId");
        
        log.info("🤖 AI: {}", response.getStr("message"));
        
        if (response.getBool("functionCalled", false)) {
            log.info("📞 函数调用: {}", response.getStr("functionName"));
            String funcResult = response.getStr("functionResult");
            if (funcResult != null && !funcResult.isEmpty()) {
                log.info("📊 调用结果: {}", 
                        funcResult.substring(0, Math.min(100, funcResult.length())) + "...");
            }
        }
        
        String error = response.getStr("error");
        if (error != null && !error.isEmpty()) {
            log.warn("⚠️ 错误: {}", error);
        }
        
        log.info("🔑 会话ID: {}", response.getStr("sessionId"));
        log.info("-----------------------------------------------------------");
        
        // 延迟避免请求过快
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        return response;
    }
    
    /**
     * 发送聊天请求
     */
    private JSONObject sendChatRequest(String message, boolean enableFunctions, String sessionId) 
            throws IOException {
        JSONObject requestJson = new JSONObject();
        requestJson.set("message", message);
        requestJson.set("enableFunctions", enableFunctions);
        if (sessionId != null) {
            requestJson.set("sessionId", sessionId);
        }
        
        RequestBody body = RequestBody.create(requestJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/ai/chat")
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("❌ HTTP 请求失败: {}", response.code());
                JSONObject errorResponse = new JSONObject();
                errorResponse.set("error", "HTTP " + response.code());
                errorResponse.set("message", "请求失败");
                return errorResponse;
            }
            
            String responseBody = response.body().string();
            return JSONUtil.parseObj(responseBody);
        }
    }
    
    /**
     * 清除会话
     */
    private void clearSession(String sessionId) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/ai/session/" + sessionId)
                .delete()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("✅ 会话已清除");
            } else {
                log.warn("⚠️ 清除会话失败: HTTP {}", response.code());
            }
        }
    }
    
    @AfterAll
    public static void teardownClass() {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("🎉 所有对话场景测试完成！");
        log.info("═══════════════════════════════════════════════════════");
        
        // 关闭 HTTP 客户端
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}

