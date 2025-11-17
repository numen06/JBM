package com.jbm.cluster.logs;

import com.jbm.cluster.api.entitys.log.BusinessLog;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.cluster.logs.service.BusinessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务日志使用示例
 * 演示文件型日志（如编译日志、构建日志）的使用
 * 
 * 注意：现在使用共享的 Form/Entity（在 jbm-cluster-api-basic 模块中）
 * 
 * @author wesley
 */
@Component
@Slf4j
public class BusinessLogExample {
    
    @Autowired
    private BusinessLogService businessLogService;
    
    /**
     * 示例1：编译日志
     * 模拟Java项目编译过程的日志记录
     */
    public void compileLogExample() {
        log.info("========== 开始测试编译日志示例 ==========");
        
        // 1. 创建编译日志
        CreateBusinessLogForm createForm = new CreateBusinessLogForm();
        createForm.setModule("构建系统");
        createForm.setOperation("Maven编译");
        createForm.setUserId("build001");
        createForm.setUsername("构建机器人");
        createForm.setContent("开始编译项目: jbm-cluster-platform\n" +
                "Maven版本: 3.8.5\n" +
                "Java版本: 1.8\n" +
                "项目路径: /workspace/jbm-cluster");
        createForm.setAutoTimestamp(true); // 启用自动时间戳
        createForm.setExpireDays(30);
        
        String logId = businessLogService.createLog(createForm);
        log.info("编译日志创建成功，logId: {}", logId);
        
        // 2. 模拟编译过程，不断追加日志
        AppendBusinessLogForm appendForm = new AppendBusinessLogForm();
        appendForm.setLogId(logId);
        appendForm.setAutoTimestamp(true);
        
        // 清理阶段
        appendForm.setContent("正在清理旧的编译文件...\n清理完成，删除文件数: 125");
        businessLogService.appendLog(appendForm);
        
        // 编译阶段
        appendForm.setContent("开始编译源代码...\n编译模块: jbm-cluster-api\n编译模块: jbm-cluster-common\n编译模块: jbm-cluster-core");
        businessLogService.appendLog(appendForm);
        
        // 测试阶段
        appendForm.setContent("运行单元测试...\n测试类: UserServiceTest - 通过\n测试类: OrderServiceTest - 通过\n测试类: PaymentServiceTest - 失败");
        businessLogService.appendLog(appendForm);
        
        // 打包阶段
        appendForm.setContent("开始打包...\n打包类型: jar\n打包文件: target/jbm-cluster-platform-1.0.0.jar\n打包大小: 45.2 MB");
        businessLogService.appendLog(appendForm);
        
        // 完成
        appendForm.setContent("编译完成！\n总耗时: 2分35秒\n状态: 成功（警告1个）");
        businessLogService.appendLog(appendForm);
        
        // 3. 查看完整编译日志
        String fullLog = businessLogService.getLogByIdFullContent(logId);
        log.info("完整编译日志：\n{}", fullLog);
        
        // 4. 查看最后10行（类似tail -n 10）
        Integer totalLines = businessLogService.getLogTotalLines(logId);
        List<BusinessLog> last10Lines = businessLogService.getLogByLineRange(logId, 
                Math.max(1, totalLines - 9), totalLines);
        log.info("最后10行日志：");
        last10Lines.forEach(line -> log.info("{} | {}", line.getLineNumber(), line.getContent()));
        
        // 5. 查看多行格式
        List<BusinessLog> allLines = businessLogService.getLogByIdMultiLine(logId);
        log.info("总行数: {}", allLines.size());
        
        log.info("========== 编译日志示例测试完成 ==========");
    }
    
    /**
     * 示例2：部署日志
     * 模拟应用部署过程的日志记录
     */
    public void deployLogExample() {
        log.info("========== 开始测试部署日志示例 ==========");
        
        // 创建部署日志
        CreateBusinessLogForm createForm = new CreateBusinessLogForm();
        createForm.setModule("部署系统");
        createForm.setOperation("应用部署");
        createForm.setUserId("deploy001");
        createForm.setUsername("部署管理员");
        createForm.setContent("开始部署应用: jbm-cluster-platform\n" +
                "目标环境: 生产环境\n" +
                "版本号: v1.2.3\n" +
                "部署服务器: 192.168.1.100");
        createForm.setAutoTimestamp(true);
        createForm.setExpireDays(60); // 保存60天
        
        String logId = businessLogService.createLog(createForm);
        log.info("部署日志创建成功，logId: {}", logId);
        
        AppendBusinessLogForm appendForm = new AppendBusinessLogForm();
        appendForm.setLogId(logId);
        appendForm.setAutoTimestamp(true);
        
        // 备份阶段
        appendForm.setContent("备份当前版本...\n备份文件: /backup/jbm-cluster-platform-v1.2.2.tar.gz\n备份大小: 128 MB\n备份完成");
        businessLogService.appendLog(appendForm);
        
        // 停止服务
        appendForm.setContent("停止当前服务...\n停止进程: PID 12345\n服务已停止");
        businessLogService.appendLog(appendForm);
        
        // 部署新版本
        appendForm.setContent("部署新版本文件...\n上传文件: jbm-cluster-platform-v1.2.3.jar\n文件大小: 45.2 MB\n上传完成");
        businessLogService.appendLog(appendForm);
        
        // 启动服务
        appendForm.setContent("启动新版本服务...\n启动命令: java -jar jbm-cluster-platform-v1.2.3.jar\n等待服务就绪...\n健康检查: http://192.168.1.100:8080/health\n服务启动成功");
        businessLogService.appendLog(appendForm);
        
        // 验证阶段
        appendForm.setContent("验证部署结果...\n接口测试: /api/users - 正常\n接口测试: /api/orders - 正常\n数据库连接: 正常\n部署验证通过");
        businessLogService.appendLog(appendForm);
        
        // 完成
        appendForm.setContent("部署完成！\n部署时间: 3分15秒\n状态: 成功");
        businessLogService.appendLog(appendForm);
        
        // 查看完整部署日志
        String fullLog = businessLogService.getLogByIdFullContent(logId);
        log.info("完整部署日志：\n{}", fullLog);
        
        log.info("========== 部署日志示例测试完成 ==========");
    }
    
    /**
     * 示例3：测试运行日志
     * 模拟自动化测试运行的日志记录
     */
    public void testRunLogExample() {
        log.info("========== 开始测试运行日志示例 ==========");
        
        CreateBusinessLogForm createForm = new CreateBusinessLogForm();
        createForm.setModule("测试系统");
        createForm.setOperation("自动化测试");
        createForm.setUserId("test001");
        createForm.setUsername("测试机器人");
        createForm.setContent("开始执行自动化测试套件\n" +
                "测试框架: JUnit 5\n" +
                "测试环境: CI/CD Pipeline\n" +
                "测试分支: feature/user-module");
        createForm.setAutoTimestamp(true);
        createForm.setExpireDays(90);
        
        String logId = businessLogService.createLog(createForm);
        log.info("测试运行日志创建成功，logId: {}", logId);
        
        AppendBusinessLogForm appendForm = new AppendBusinessLogForm();
        appendForm.setLogId(logId);
        appendForm.setAutoTimestamp(true);
        
        // 准备阶段
        appendForm.setContent("准备测试环境...\n初始化数据库\n加载测试数据\n启动Mock服务");
        businessLogService.appendLog(appendForm);
        
        // 执行测试
        appendForm.setContent("执行单元测试...\nUserServiceTest - 通过 (15ms)\nOrderServiceTest - 通过 (23ms)\nPaymentServiceTest - 失败 (45ms)");
        businessLogService.appendLog(appendForm);
        
        // 详细错误信息
        appendForm.setContent("测试失败详情:\n" +
                "测试类: PaymentServiceTest.paymentProcessTest\n" +
                "错误信息: java.lang.NullPointerException\n" +
                "堆栈跟踪: at PaymentService.processPayment(PaymentService.java:45)");
        businessLogService.appendLog(appendForm);
        
        // 测试总结
        appendForm.setContent("测试执行完成\n" +
                "总测试数: 156\n" +
                "通过: 155\n" +
                "失败: 1\n" +
                "跳过: 0\n" +
                "总耗时: 2分35秒");
        businessLogService.appendLog(appendForm);
        
        // 查看测试日志
        String fullLog = businessLogService.getLogByIdFullContent(logId);
        log.info("完整测试运行日志：\n{}", fullLog);
        
        // 查看失败的部分（假设失败在最后几行）
        Integer totalLines = businessLogService.getLogTotalLines(logId);
        List<BusinessLog> errorLines = businessLogService.getLogByLineRange(logId, 
                Math.max(1, totalLines - 5), totalLines);
        log.info("最后5行（包含错误信息）：");
        errorLines.forEach(line -> log.info("{} | {}", line.getLineNumber(), line.getContent()));
        
        log.info("========== 测试运行日志示例测试完成 ==========");
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        try {
            compileLogExample();
            Thread.sleep(1000);
            
            deployLogExample();
            Thread.sleep(1000);
            
            testRunLogExample();
            
            log.info("========== 所有示例测试完成 ==========");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("测试被中断", e);
        }
    }
}

