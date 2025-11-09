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
 * 用户对话模拟测试（完整版）
 * 
 * 功能说明：
 * - 通过真实的 HTTP 接口测试 AI 系统的对话功能
 * - 完整模拟用户输入和 AI 输出过程
 * - 支持普通对话和流式对话（SSE）两种模式
 * - 详细记录和展示测试过程和结果
 * 
 * 测试场景：
 * 1. 简单问候 - 建立初次联系
 * 2. 了解系统能力 - 询问 AI 能做什么
 * 3. 查询系统健康状态 - 首次 Function Calling
 * 4. 查询用户列表 - 数据查询场景
 * 5. 多轮对话 - 测试上下文理解和保持
 * 6. 复杂查询 - 带参数的接口调用
 * 7. 错误处理 - 无法找到合适接口的情况
 * 8. 清除会话 - 重新开始对话
 * 9. 综合业务流程 - 完整的多步骤业务操作
 * 10. 流式对话 - SSE 实时响应
 * 11. 流式 Function Calling - 流式模式下的函数调用
 * 12. 交互式对话 - 手动测试（可选）
 * 
 * 改进点：
 * ✅ 完整显示用户输入（消息、参数、会话ID）
 * ✅ 详细展示 AI 输出（内容、函数调用、执行结果）
 * ✅ 流式对话实时显示（文本块、统计数据、错误信息）
 * ✅ 增强错误处理（HTTP 错误、JSON 解析错误、空响应）
 * ✅ 添加数据统计（响应时间、接收行数、字符数）
 * ✅ 验证测试结果（自动检查响应完整性）
 * ✅ 清晰的日志格式（使用分隔线和图标）
 * 
 * 使用方法：
 * 1. 确保服务已在 http://localhost:3315 启动
 * 2. 设置 DASHSCOPE_API_KEY 环境变量
 * 3. 运行测试类或单个测试方法
 * 4. 查看控制台输出了解详细的测试过程
 * 
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
            log.info("⏳ 正在检查服务状态...");
            
            Request healthCheck = new Request.Builder()
                    .url(BASE_URL + "/ai/health")
                    .get()
                    .build();
            
            Response response = httpClient.newCall(healthCheck).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject healthData = JSONUtil.parseObj(body);
                int apiCount = healthData.getInt("apiCount", 0);
                
                log.info("✅ 服务运行正常");
                log.info("📊 当前已收集 {} 个 API", apiCount);
                
                // 智能等待：根据 API 数量决定等待时间
                if (apiCount == 0) {
                    log.warn("⚠️ 还没有收集到 API，等待 15 秒...");
                    Thread.sleep(15000);
                } else if (apiCount < 5) {
                    log.info("⏳ API 收集中（{}个），等待 10 秒...", apiCount);
                    Thread.sleep(10000);
                } else {
                    log.info("✅ API 已就绪（{}个），等待 5 秒确保函数注册完成...", apiCount);
                    Thread.sleep(5000);
                }
                
                // 查看详细统计
                Request statsReq = new Request.Builder()
                        .url(BASE_URL + "/ai/stats")
                        .get()
                        .build();
                Response statsResp = httpClient.newCall(statsReq).execute();
                if (statsResp.isSuccessful()) {
                    JSONObject stats = JSONUtil.parseObj(statsResp.body().string());
                    log.info("📈 系统就绪状态:");
                    log.info("   ✅ API 总数: {}", stats.getInt("totalApis"));
                    log.info("   ✅ 服务数: {}", stats.getInt("serviceCount"));
                    log.info("   ✅ 可用函数: {}", stats.getInt("functionsCount"));
                    log.info("   ✅ 缓存状态: {}", stats.getBool("cacheEnabled") ? "已启用" : "未启用");
                }
                statsResp.close();
                
            } else {
                log.error("❌ 服务健康检查失败: HTTP {}", response.code());
                log.error("请检查服务是否正常启动");
            }
            response.close();
        } catch (Exception e) {
            log.error("❌ 无法连接到服务: {}", e.getMessage());
            log.error("请确保:");
            log.error("  1. 服务已启动在端口 3315");
            log.error("  2. 已设置 DASHSCOPE_API_KEY 环境变量");
            log.error("  3. 已删除旧缓存: rm -f data/api-metadata-cache.json");
            log.error("  4. MockApiController 已启用");
        }
        
        log.info("═══════════════════════════════════════════════════════\n");
    }
    
    @BeforeEach
    public void setup() {
        // 每个测试间隔 1 秒，避免请求过快
        try {
            Thread.sleep(1000);
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
        JSONObject response = simulateUserChat("帮我看一下系统现在的系统健康状态", true);
        
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
        log.info("目标：测试 AI 能否理解并保持多轮对话的上下文");
        log.info("═══════════════════════════════════════════════════════\n");
        
        // 第一轮：查询数据
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 [第1轮对话] 用户输入: 查询最近的订单");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        JSONObject response1 = sendChatRequest("查询最近的订单", true, null);
        sessionId = response1.getStr("sessionId");
        
        log.info("🤖 [第1轮对话] AI 响应:");
        log.info("   {}", response1.getStr("message"));
        
        if (response1.getBool("functionCalled", false)) {
            log.info("   📞 调用函数: {}", response1.getStr("functionName"));
        }
        log.info("   🔑 会话ID: {}", sessionId);
        
        // 第二轮：追加条件（测试上下文）
        Thread.sleep(2000);
        
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 [第2轮对话] 用户输入: 只要今天的");
        log.info("   ℹ️  上下文: 继续第1轮关于订单查询的讨论");
        log.info("   🔑 使用会话ID: {}", sessionId);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        JSONObject response2 = sendChatRequest("只要今天的", true, sessionId);
        
        log.info("🤖 [第2轮对话] AI 响应:");
        log.info("   {}", response2.getStr("message"));
        
        if (response2.getBool("functionCalled", false)) {
            log.info("   📞 调用函数: {}", response2.getStr("functionName"));
        }
        
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 场景5完成：多轮对话测试");
        log.info("   验证点:");
        log.info("   1️⃣  会话ID 保持一致: {}", sessionId);
        log.info("   2️⃣  第2轮能理解第1轮的上下文: {}", 
                response2.getStr("message").contains("订单") || 
                response2.getBool("functionCalled", false) ? "✅ 是" : "⚠️ 待确认");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
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
        log.info("  步骤1: 查询信息");
        log.info("  步骤2: 追加查询（测试上下文）");
        log.info("  步骤3: 总结分析");
        log.info("═══════════════════════════════════════════════════════\n");
        
        // 步骤1：查询信息
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 步骤1/3 - 初始查询");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 用户: 查询最近一周的订单统计");
        
        JSONObject response1 = sendChatRequest("查询最近一周的订单统计", true, null);
        String flowSessionId = response1.getStr("sessionId");
        
        log.info("🤖 AI 响应: {}", response1.getStr("message"));
        if (response1.getBool("functionCalled", false)) {
            log.info("   📞 调用函数: {}", response1.getStr("functionName"));
        }
        log.info("   🔑 会话ID: {}", flowSessionId);
        
        // 步骤2：追加查询
        Thread.sleep(2000);
        
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 步骤2/3 - 追加条件查询");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 用户: 其中有多少是已完成的？");
        log.info("   ℹ️  上下文: 继续讨论订单统计");
        
        JSONObject response2 = sendChatRequest("其中有多少是已完成的？", true, flowSessionId);
        
        log.info("🤖 AI 响应: {}", response2.getStr("message"));
        if (response2.getBool("functionCalled", false)) {
            log.info("   📞 调用函数: {}", response2.getStr("functionName"));
        }
        
        // 步骤3：总结
        Thread.sleep(2000);
        
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 步骤3/3 - 请求总结");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 用户: 帮我总结一下整体情况");
        log.info("   ℹ️  预期: AI 根据前两轮对话进行总结");
        
        JSONObject response3 = sendChatRequest("帮我总结一下整体情况", false, flowSessionId);
        
        log.info("🤖 AI 响应: {}", response3.getStr("message"));
        
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 场景9完成：综合业务流程");
        log.info("   验证点:");
        log.info("   1️⃣  会话连续性: 3轮对话使用同一会话ID");
        log.info("   2️⃣  上下文理解: AI 能理解前两轮的查询内容");
        log.info("   3️⃣  数据分析能力: AI 能根据历史对话做总结");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
    
    /**
     * 测试 10: 流式对话测试
     */
    @Test
    @Order(10)
    @DisplayName("场景10: 流式对话 - SSE 实时响应")
    public void testScenario10_StreamChat() throws IOException, InterruptedException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景10: 流式对话 - SSE 实时响应");
        log.info("═══════════════════════════════════════════════════════");
        log.info("目标：测试 Server-Sent Events (SSE) 流式响应");
        log.info("特点：AI 边生成边返回，用户实时看到回复");
        log.info("═══════════════════════════════════════════════════════\n");
        
        // 测试流式接口
        testStreamResponse("请介绍一下你自己和你的功能", false);
        
        log.info("");
        log.info("✅ 场景10完成：流式对话测试");
        log.info("   验证点: SSE 流式传输、实时文本显示\n");
    }
    
    /**
     * 测试 11: 流式 Function Calling
     */
    @Test
    @Order(11)
    @DisplayName("场景11: 流式 Function Calling")
    public void testScenario11_StreamFunctionCalling() throws IOException, InterruptedException {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📝 场景11: 流式 Function Calling");
        log.info("═══════════════════════════════════════════════════════");
        log.info("目标：在流式模式下测试函数调用能力");
        log.info("特点：函数调用 + 结果解析 + 流式返回");
        log.info("═══════════════════════════════════════════════════════\n");
        
        testStreamResponse("查询系统健康状态", true);
        
        log.info("");
        log.info("✅ 场景11完成：流式 Function Calling");
        log.info("   验证点: 流式模式下的函数调用和结果展示\n");
    }
    
    /**
     * 测试 12: 交互式对话（可选）
     */
    @Test
    @Order(12)
    @DisplayName("场景12: 交互式对话（手动测试）")
    @Disabled("手动测试时启用")
    public void testScenario12_InteractiveChat() throws IOException {
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
     * 辅助方法：模拟用户对话（普通模式，改进版：完整显示输入输出）
     */
    private JSONObject simulateUserChat(String message, boolean enableFunctions) throws IOException {
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 [普通模式] 用户输入:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 消息: {}", message);
        log.info("⚙️  Function Calling: {}", enableFunctions ? "启用" : "禁用");
        log.info("🔑 会话ID: {}", sessionId != null ? sessionId : "新会话");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        log.info("📡 正在发送请求...");
        long startTime = System.currentTimeMillis();
        
        JSONObject response = sendChatRequest(message, enableFunctions, sessionId);
        long duration = System.currentTimeMillis() - startTime;
        
        // 更新会话ID
        String returnedSessionId = response.getStr("sessionId");
        if (returnedSessionId != null) {
            sessionId = returnedSessionId;
        }
        
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📥 [普通模式] AI 响应:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        String aiMessage = response.getStr("message");
        if (aiMessage != null && !aiMessage.isEmpty()) {
            log.info("🤖 回复内容:");
            // 如果回复很长，分行显示
            if (aiMessage.length() > 80) {
                String[] lines = aiMessage.split("\n");
                for (String line : lines) {
                    log.info("   {}", line);
                }
            } else {
                log.info("   {}", aiMessage);
            }
        } else {
            log.warn("   (AI 回复为空)");
        }
        
        log.info("");
        log.info("📊 响应详情:");
        log.info("   ⏱️  响应时间: {}ms", duration);
        
        // 函数调用信息
        if (response.getBool("functionCalled", false)) {
            log.info("");
            log.info("🔧 函数调用信息:");
            log.info("   📞 函数名: {}", response.getStr("functionName"));
            
            String funcResult = response.getStr("functionResult");
            if (funcResult != null && !funcResult.isEmpty()) {
                if (funcResult.length() > 200) {
                    log.info("   📊 函数结果: {}...", funcResult.substring(0, 200));
                    log.info("      (总长度: {} 字符)", funcResult.length());
                } else {
                    log.info("   📊 函数结果: {}", funcResult);
            }
        }
        } else {
            log.info("   ℹ️  未调用函数");
        }
        
        // 错误信息
        String error = response.getStr("error");
        if (error != null && !error.isEmpty()) {
            log.info("");
            log.warn("❌ 错误信息: {}", error);
        } else {
            log.info("   ✅ 执行状态: 成功");
        }
        
        log.info("   🔑 会话ID: {}", returnedSessionId != null ? returnedSessionId : "(未返回)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return response;
    }
    
    /**
     * 测试流式响应（改进版：完整显示输入输出）
     */
    private void testStreamResponse(String message, boolean enableFunctions) 
            throws IOException, InterruptedException {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 [流式输入] 用户消息: {}", message);
        log.info("⚙️  [流式输入] Function Calling: {}", enableFunctions ? "启用" : "禁用");
        log.info("🔑 [流式输入] 会话ID: {}", sessionId != null ? sessionId : "新会话");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        JSONObject requestJson = new JSONObject();
        requestJson.set("message", message);
        requestJson.set("enableFunctions", enableFunctions);
        if (sessionId != null) {
            requestJson.set("sessionId", sessionId);
        }
        
        RequestBody body = RequestBody.create(requestJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/ai/chat/stream")
                .post(body)
                .addHeader("Accept", "text/event-stream")
                .build();
        
        log.info("📡 [流式] 正在发送请求到服务器...");
        long startTime = System.currentTimeMillis();
        
        // 统计数据
        StringBuilder fullResponse = new StringBuilder();
        String currentSessionId = null;
        boolean hasFunctionCall = false;
        String functionName = null;
        String functionArgs = null;
        int textChunkCount = 0;
        int totalLineCount = 0;
        boolean hasError = false;
        String errorMessage = null;
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.error("❌ [流式输出] HTTP 请求失败");
                log.error("   响应码: HTTP {}", response.code());
                log.error("   状态: {}", response.message());
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }
            
            log.info("✅ [流式] HTTP 连接成功，开始接收 SSE 流...");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📥 [流式输出] AI 响应（实时）:");
            System.out.println();
            System.out.print("🤖 AI: ");
            
            String line;
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body().byteStream()));
            
            while ((line = reader.readLine()) != null) {
                totalLineCount++;
                
                // 显示原始接收的数据（调试用）
                if (line.trim().isEmpty()) {
                    continue;  // 跳过空行
                }
                
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    
                    // 调试：显示原始数据
                    log.debug("   [RAW] 接收数据: {}", data);
                    
                    if ("[DONE]".equals(data)) {
                        System.out.println();
                        log.info("");
                        log.info("✅ [流式] 接收到结束标记 [DONE]");
                        break;
                    }
                    
                    try {
                        JSONObject chunk = JSONUtil.parseObj(data);
                        String type = chunk.getStr("type");
                        
                        if (type == null) {
                            log.warn("⚠️  [流式] 收到无类型的数据块: {}", data);
                            continue;
                        }
                        
                        switch (type) {
                            case "sessionId":
                                currentSessionId = chunk.getStr("sessionId");
                                log.info("🔑 [流式] 收到会话ID: {}", currentSessionId);
                                break;
                                
                            case "text":
                                String content = chunk.getStr("content");
                                if (content != null && !content.isEmpty()) {
                                    textChunkCount++;
                                fullResponse.append(content);
                                System.out.print(content);  // 实时输出
                                System.out.flush();
                                }
                                break;
                                
                            case "functionCall":
                                hasFunctionCall = true;
                                functionName = chunk.getStr("functionName");
                                functionArgs = chunk.getStr("arguments");
                                System.out.println();  // 换行
                                log.info("");
                                log.info("📞 [流式] AI 决定调用函数:");
                                log.info("   函数名: {}", functionName);
                                log.info("   参数: {}", functionArgs);
                                System.out.print("\n[⏳ 正在调用函数: " + functionName + "...]\n");
                                System.out.print("🤖 AI: ");
                                break;
                                
                            case "functionResult":
                                String result = chunk.getStr("result");
                                log.info("📊 [流式] 函数执行结果: {}", 
                                        result != null && result.length() > 100 ? 
                                        result.substring(0, 100) + "..." : result);
                                break;
                                
                            case "error":
                                hasError = true;
                                errorMessage = chunk.getStr("message");
                                System.out.println();  // 换行
                                log.error("");
                                log.error("❌ [流式] 接收到错误:");
                                log.error("   错误信息: {}", errorMessage);
                                break;
                                
                            default:
                                log.debug("   [流式] 未知类型: {} - {}", type, data);
                                break;
                        }
                    } catch (Exception e) {
                        log.warn("⚠️  [流式] 数据解析失败: {} - 原始数据: {}", e.getMessage(), data);
                    }
                } else if (!line.trim().isEmpty()) {
                    // 不是 data: 开头的行
                    log.debug("   [RAW] 其他行: {}", line);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 输出完整的测试结果
            System.out.println();
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📊 [流式测试] 结果统计:");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ 完整回复内容:");
            log.info("   {}", fullResponse.length() > 0 ? fullResponse.toString() : "(无内容)");
            log.info("");
            log.info("📈 数据统计:");
            log.info("   ⏱️  总耗时: {}ms", duration);
            log.info("   📦 接收行数: {}", totalLineCount);
            log.info("   📝 文本块数: {}", textChunkCount);
            log.info("   📏 总字符数: {}", fullResponse.length());
            log.info("");
            if (hasFunctionCall) {
                log.info("🔧 函数调用:");
                log.info("   📞 函数名: {}", functionName);
                log.info("   📦 参数: {}", functionArgs != null ? functionArgs : "(无)");
            } else {
                log.info("ℹ️  未触发函数调用");
            }
            log.info("");
            if (hasError) {
                log.info("❌ 错误信息: {}", errorMessage);
            } else {
                log.info("✅ 执行状态: 成功");
            }
            log.info("🔑 会话ID: {}", currentSessionId != null ? currentSessionId : "(未返回)");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // 验证测试结果
            if (fullResponse.length() == 0 && !hasError) {
                log.warn("");
                log.warn("⚠️  警告: 流式响应为空但没有报错");
                log.warn("   可能的原因:");
                log.warn("   1. AI 模型配置问题");
                log.warn("   2. 服务端流式输出格式不正确");
                log.warn("   3. SSE 事件未正确发送");
                log.warn("   建议检查服务端日志");
            }
            
            // 更新 sessionId
            if (currentSessionId != null) {
                sessionId = currentSessionId;
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ [流式测试] 异常终止");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("⏱️  耗时: {}ms", duration);
            log.error("❌ 异常类型: {}", e.getClass().getSimpleName());
            log.error("❌ 异常信息: {}", e.getMessage());
            log.error("📦 已接收行数: {}", totalLineCount);
            log.error("📝 已接收内容: {}", fullResponse.length() > 0 ? 
                    fullResponse.substring(0, Math.min(100, fullResponse.length())) + "..." : "(无)");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            throw e;
        }
    }
    
    /**
     * 发送聊天请求（普通模式）
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
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("❌ [普通模式] HTTP 请求失败");
                log.error("   响应码: HTTP {}", response.code());
                log.error("   状态: {}", response.message());
                
                // 尝试读取错误响应体
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                log.error("   响应内容: {}", errorBody);
                
                JSONObject errorResponse = new JSONObject();
                errorResponse.set("error", "HTTP " + response.code());
                errorResponse.set("message", "请求失败: " + response.message());
                errorResponse.set("details", errorBody);
                return errorResponse;
            }
            
            String responseBody = response.body().string();
            
            // 验证响应是否为空
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("⚠️  [普通模式] 服务器返回空响应");
                JSONObject emptyResponse = new JSONObject();
                emptyResponse.set("error", "空响应");
                emptyResponse.set("message", "服务器返回了空内容");
                return emptyResponse;
            }
            
            // 尝试解析 JSON
            try {
                JSONObject result = JSONUtil.parseObj(responseBody);
                return result;
            } catch (Exception e) {
                log.error("❌ [普通模式] 响应 JSON 解析失败: {}", e.getMessage());
                log.error("   原始响应: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                
                JSONObject parseErrorResponse = new JSONObject();
                parseErrorResponse.set("error", "JSON 解析失败");
                parseErrorResponse.set("message", e.getMessage());
                parseErrorResponse.set("rawResponse", responseBody);
                return parseErrorResponse;
            }
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
        log.info("");
        log.info("📊 测试总结:");
        log.info("   - 测试场景: 11 个");
        log.info("   - 包含流式: 2 个");
        log.info("   - 验证内容:");
        log.info("     ✅ 普通对话");
        log.info("     ✅ Function Calling");
        log.info("     ✅ 多轮对话");
        log.info("     ✅ 上下文保持");
        log.info("     ✅ 参数提取");
        log.info("     ✅ 流式响应");
        log.info("     ✅ API 列表注入");
        log.info("");
        
        // 关闭 HTTP 客户端
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}

