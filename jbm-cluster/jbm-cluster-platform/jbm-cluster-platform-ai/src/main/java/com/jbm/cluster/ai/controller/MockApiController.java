package com.jbm.cluster.ai.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 模拟 API 控制器
 * 用于测试 AI Function Calling 功能
 * 提供简单的模拟接口，快速验证 AI 调用流程
 * 
 * @author wesley
 */
@Api(tags = "模拟接口（测试用）")
@RestController
@RequestMapping("/mock")
@Slf4j
public class MockApiController {

    /**
     * 获取在线用户列表
     */
    @ApiOperation("获取当前在线用户列表")
    @GetMapping("/users/online")
    public Map<String, Object> getOnlineUsers() {
        log.info("📞 [Mock API] 调用: 获取在线用户");
        
        List<Map<String, Object>> users = Arrays.asList(
                Map.of("id", 1, "name", "张三", "status", "online", "loginTime", "2024-11-10 10:30:00"),
                Map.of("id", 2, "name", "李四", "status", "online", "loginTime", "2024-11-10 09:15:00"),
                Map.of("id", 3, "name", "王五", "status", "online", "loginTime", "2024-11-10 11:20:00"),
                Map.of("id", 4, "name", "赵六", "status", "online", "loginTime", "2024-11-10 08:45:00"),
                Map.of("id", 5, "name", "钱七", "status", "online", "loginTime", "2024-11-10 10:05:00")
        );
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "total", users.size(),
                        "list", users
                )
        );
    }
    
    /**
     * 获取用户列表
     */
    @ApiOperation("获取所有用户列表")
    @GetMapping("/users/list")
    public Map<String, Object> getUserList(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("📞 [Mock API] 调用: 获取用户列表, page={}, pageSize={}", page, pageSize);
        
        List<Map<String, Object>> users = new ArrayList<>();
        for (int i = 1; i <= pageSize; i++) {
            users.add(Map.of(
                    "id", (page - 1) * pageSize + i,
                    "name", "用户" + ((page - 1) * pageSize + i),
                    "email", "user" + ((page - 1) * pageSize + i) + "@example.com",
                    "status", i % 2 == 0 ? "online" : "offline",
                    "createTime", "2024-10-" + (10 + i % 20)
            ));
        }
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "total", 100,
                        "page", page,
                        "pageSize", pageSize,
                        "list", users
                )
        );
    }
    
    /**
     * 获取用户详情
     */
    @ApiOperation("根据ID获取用户详细信息")
    @GetMapping("/users/{id}")
    public Map<String, Object> getUserDetail(
            @ApiParam("用户ID") @PathVariable Integer id) {
        
        log.info("📞 [Mock API] 调用: 获取用户详情, id={}", id);
        
        Map<String, Object> user = Map.of(
                "id", id,
                "name", "用户" + id,
                "email", "user" + id + "@example.com",
                "phone", "138****" + String.format("%04d", id),
                "status", id % 2 == 0 ? "online" : "offline",
                "loginCount", id * 10,
                "lastLoginTime", LocalDateTime.now().minusHours(id % 24).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "createTime", "2024-" + String.format("%02d", (id % 12) + 1) + "-15",
                "role", id % 3 == 0 ? "admin" : "user"
        );
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", user
        );
    }
    
    /**
     * 获取订单列表
     */
    @ApiOperation("获取订单列表")
    @GetMapping("/orders/list")
    public Map<String, Object> getOrderList(
            @ApiParam("日期筛选") @RequestParam(required = false) String date,
            @ApiParam("状态筛选") @RequestParam(required = false) String status) {
        
        log.info("📞 [Mock API] 调用: 获取订单列表, date={}, status={}", date, status);
        
        List<Map<String, Object>> orders = new ArrayList<>();
        int totalCount = 150;
        
        // 根据筛选条件调整数量
        if ("today".equals(date) || "2024-11-10".equals(date)) {
            totalCount = 12;
        }
        
        if ("completed".equals(status)) {
            totalCount = (int) (totalCount * 0.6);
        }
        
        for (int i = 1; i <= Math.min(totalCount, 10); i++) {
            orders.add(Map.of(
                    "id", 1000 + i,
                    "orderNo", "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%04d", i),
                    "amount", (i * 100) + (Math.random() * 500),
                    "status", i % 3 == 0 ? "completed" : (i % 3 == 1 ? "pending" : "processing"),
                    "createTime", LocalDateTime.now().minusHours(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "userId", i
            ));
        }
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "total", totalCount,
                        "completed", (int) (totalCount * 0.6),
                        "pending", (int) (totalCount * 0.2),
                        "processing", (int) (totalCount * 0.2),
                        "list", orders
                )
        );
    }
    
    /**
     * 获取订单统计
     */
    @ApiOperation("获取订单统计信息")
    @GetMapping("/orders/stats")
    public Map<String, Object> getOrderStats(
            @ApiParam("统计周期(day/week/month)") @RequestParam(defaultValue = "week") String period) {
        
        log.info("📞 [Mock API] 调用: 获取订单统计, period={}", period);
        
        int total = "day".equals(period) ? 12 : ("week".equals(period) ? 85 : 350);
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "period", period,
                        "total", total,
                        "completed", (int) (total * 0.65),
                        "pending", (int) (total * 0.15),
                        "processing", (int) (total * 0.15),
                        "cancelled", (int) (total * 0.05),
                        "totalAmount", total * 258.5,
                        "avgAmount", 258.5
                )
        );
    }
    
    /**
     * 获取系统健康状态
     */
    @ApiOperation("获取系统健康状态")
    @GetMapping("/system/health")
    public Map<String, Object> getSystemHealth() {
        log.info("📞 [Mock API] 调用: 获取系统健康状态");
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "status", "UP",
                        "components", Map.of(
                                "database", Map.of("status", "UP", "responseTime", "15ms"),
                                "redis", Map.of("status", "UP", "responseTime", "2ms"),
                                "nacos", Map.of("status", "UP", "responseTime", "10ms"),
                                "diskSpace", Map.of("status", "UP", "free", "150GB", "total", "500GB")
                        ),
                        "uptime", "15天3小时42分钟",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
        );
    }
    
    /**
     * 获取系统信息
     */
    @ApiOperation("获取系统运行信息")
    @GetMapping("/system/info")
    public Map<String, Object> getSystemInfo() {
        log.info("📞 [Mock API] 调用: 获取系统信息");
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "version", "7.2.21-SNAPSHOT",
                        "environment", "jaja",
                        "javaVersion", "21.0.9",
                        "springBootVersion", "2.7.18",
                        "uptime", "15天3小时42分钟",
                        "cpu", Map.of("usage", "25%", "cores", 8),
                        "memory", Map.of("used", "2.5GB", "total", "16GB", "usage", "15.6%"),
                        "activeConnections", 127,
                        "totalRequests", 125678
                )
        );
    }
    
    /**
     * 创建用户
     */
    @ApiOperation("创建新用户")
    @PostMapping("/users/create")
    public Map<String, Object> createUser(
            @ApiParam("用户名") @RequestParam String username,
            @ApiParam("邮箱") @RequestParam(required = false) String email) {
        
        log.info("📞 [Mock API] 调用: 创建用户, username={}, email={}", username, email);
        
        int newId = new Random().nextInt(9000) + 1000;
        
        return Map.of(
                "code", 200,
                "message", "创建成功",
                "data", Map.of(
                        "id", newId,
                        "username", username,
                        "email", email != null ? email : username + "@example.com",
                        "createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
        );
    }
    
    /**
     * 获取日志信息
     */
    @ApiOperation("获取系统日志")
    @GetMapping("/logs/list")
    public Map<String, Object> getLogs(
            @ApiParam("日志级别") @RequestParam(required = false) String level,
            @ApiParam("日期") @RequestParam(required = false) String date) {
        
        log.info("📞 [Mock API] 调用: 获取日志, level={}, date={}", level, date);
        
        List<Map<String, Object>> logs = new ArrayList<>();
        String[] levels = "error".equals(level) ? new String[]{"ERROR"} : new String[]{"INFO", "WARN", "ERROR"};
        
        for (int i = 1; i <= 10; i++) {
            String logLevel = levels[i % levels.length];
            logs.add(Map.of(
                    "id", i,
                    "level", logLevel,
                    "message", "这是一条" + logLevel + "级别的日志消息 #" + i,
                    "timestamp", LocalDateTime.now().minusMinutes(i * 5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "service", i % 2 == 0 ? "jbm-cluster-platform-center" : "jbm-cluster-platform-auth"
            ));
        }
        
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "total", "error".equals(level) ? 3 : 25,
                        "list", logs
                )
        );
    }
    
    /**
     * 测试健康检查
     */
    @ApiOperation("测试健康检查接口")
    @GetMapping("/test/health")
    public Map<String, Object> testHealth() {
        log.info("📞 [Mock API] 调用: 测试健康检查");
        
        return Map.of(
                "status", "ok",
                "service", "jbm-ai-mock",
                "message", "模拟接口运行正常",
                "timestamp", System.currentTimeMillis()
        );
    }
}

