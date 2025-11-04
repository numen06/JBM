package com.jbm.cluster.logs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * 业务日志功能测试类
 * 测试文件型日志（编译日志、构建日志等）的完整功能
 * 实现CommandLineRunner，应用启动后自动运行测试
 * 
 * @author wesley
 */
@Slf4j
public class BusinessLogTest implements CommandLineRunner {
    
    @Override
    public void run(String... args) {
        // 注释掉自动运行，需要时可以取消注释
        // 启动后自动运行测试
    }

}

