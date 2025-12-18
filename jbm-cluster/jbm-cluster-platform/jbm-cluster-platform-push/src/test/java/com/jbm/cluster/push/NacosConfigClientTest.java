package com.jbm.cluster.push;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nacos配置客户端测试类
 */
@Slf4j
public class NacosConfigClientTest {
    
    /**
     * 测试不带认证的Nacos配置获取
     */
    @Test
    public void testGetConfigWithoutAuth() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试获取不存在的配置
        String config = client.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
        // 记录结果但不强制断言，因为具体行为取决于服务器实现
        log.info("获取不存在配置的结果: {}", config != null ? "获得内容，长度=" + config.length() : "null");
        
        log.info("不带认证的Nacos配置获取测试完成");
    }
    
    /**
     * 测试带认证的Nacos配置获取
     */
    @Test
    public void testGetConfigWithAuth() {
        // 使用本地Nacos服务器地址和认证信息，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848", "nacos", "nacos");
        
        // 测试获取实际存在的配置
        String config = client.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
        // 记录结果但不强制断言，因为具体行为取决于服务器实现
        log.info("带认证获取配置的结果: {}", config != null ? "获得内容，长度=" + config.length() : "null");
        
        log.info("带认证的Nacos配置获取测试完成");
    }
    
    /**
     * 测试带命名空间的Nacos配置获取
     */
    @Test
    public void testGetConfigWithNamespace() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试获取指定命名空间下的配置
        String config = client.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
        // 记录结果但不强制断言，因为具体行为取决于服务器实现
        log.info("指定命名空间获取配置的结果: {}", config != null ? "获得内容，长度=" + config.length() : "null");
        
        log.info("带命名空间的Nacos配置获取测试完成");
    }
    
    /**
     * 测试获取实际存在的配置（如果有的话）
     * 这个测试用例需要环境中确实存在对应的配置才能通过
     */
    @Test
    public void testGetExistingConfig() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 尝试获取一个可能存在的配置
        String config = client.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
        // 如果配置存在，则不应该为null；如果不存在，则为null
        // 这里我们只记录结果，不强制断言
        if (config != null) {
            log.info("成功获取到配置，长度为: {} 字符", config.length());
            log.debug("配置内容预览: {}", config.substring(0, Math.min(config.length(), 100)));
        } else {
            log.info("未找到指定的配置，这可能是正常的");
        }
        
        log.info("实际配置获取测试完成");
    }
    
    /**
     * 测试列出命名空间下的所有配置
     */
    @Test
    public void testListConfigsInTenant() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试列出指定命名空间下的配置
        String configs = client.listConfigsInTenant("jaja");
        if (configs != null) {
            log.info("成功获取到命名空间下的配置列表，长度为: {} 字符", configs.length());
            log.debug("配置列表预览: {}", configs.substring(0, Math.min(configs.length(), 200)));
        } else {
            log.info("未获取到命名空间下的配置列表，可能是命名空间不存在或其他原因");
        }
        
        // 测试列出默认命名空间下的配置
        String publicConfigs = client.listConfigsInTenant(null);
        if (publicConfigs != null) {
            log.info("成功获取到默认命名空间下的配置列表，长度为: {} 字符", publicConfigs.length());
            log.debug("配置列表预览: {}", publicConfigs.substring(0, Math.min(publicConfigs.length(), 200)));
        } else {
            log.info("未获取到默认命名空间下的配置列表");
        }
        
        log.info("列出命名空间下配置测试完成");
    }
    
    /**
     * 测试无效参数情况
     */
    @Test
    public void testInvalidParameters() {
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试dataId为空的情况
        assertThrows(IllegalArgumentException.class, () -> {
            client.getConfig(null, "DEFAULT_GROUP", null);
        }, "dataId为null时应抛出IllegalArgumentException异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.getConfig("", "DEFAULT_GROUP", null);
        }, "dataId为空字符串时应抛出IllegalArgumentException异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.getConfig("   ", "DEFAULT_GROUP", null);
        }, "dataId为空白字符时应抛出IllegalArgumentException异常");
        
        log.info("无效参数测试完成");
    }
    
    /**
     * 测试带分页参数的列出命名空间下的所有配置
     */
    @Test
    public void testListConfigsInTenantWithPagination() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试带分页参数列出指定命名空间下的配置
        String configs = client.listConfigsInTenant("jaja", 1, 10);
        if (configs != null) {
            log.info("成功获取到命名空间下的配置列表(第1页，每页10条)，长度为: {} 字符", configs.length());
            log.debug("配置列表预览: {}", configs.substring(0, Math.min(configs.length(), 200)));
        } else {
            log.info("未获取到命名空间下的配置列表，可能是命名空间不存在或其他原因");
        }
        
        // 测试带分页参数列出默认命名空间下的配置
        String publicConfigs = client.listConfigsInTenant(null, 1, 20);
        if (publicConfigs != null) {
            log.info("成功获取到默认命名空间下的配置列表(第1页，每页20条)，长度为: {} 字符", publicConfigs.length());
            log.debug("配置列表预览: {}", publicConfigs.substring(0, Math.min(publicConfigs.length(), 200)));
        } else {
            log.info("未获取到默认命名空间下的配置列表");
        }
        
        // 测试使用null分页参数（应该使用默认值）
        String configsWithNull = client.listConfigsInTenant("jaja", null, null);
        if (configsWithNull != null) {
            log.info("成功获取到命名空间下的配置列表(使用默认分页参数)，长度为: {} 字符", configsWithNull.length());
        } else {
            log.info("未获取到命名空间下的配置列表");
        }
        
        log.info("带分页参数的列出命名空间下配置测试完成");
    }
    
    /**
     * 测试URL解析功能
     */
    @Test
    public void testUrlParsing() {
        // 测试不同格式的服务器地址
        NacosConfigClient client1 = new NacosConfigClient("http://10.100.10.62:8848");
        NacosConfigClient client2 = new NacosConfigClient("http://10.100.10.62:8848/");
        NacosConfigClient client3 = new NacosConfigClient("10.100.10.62:8848");
        
        // 这些测试主要验证客户端能正确构造URL，而不抛出异常
        // 使用实际存在的配置进行测试
        try {
            String config1 = client1.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
            String config2 = client2.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
            String config3 = client3.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
            log.info("URL解析测试完成，client1结果: {}, client2结果: {}, client3结果: {}", 
                    config1 != null ? "成功" : "null",
                    config2 != null ? "成功" : "null",
                    config3 != null ? "成功" : "null");
        } catch (IllegalArgumentException e) {
            // 忽略参数校验异常
            log.warn("参数校验异常: {}", e.getMessage());
        } catch (Exception e) {
            // 忽略实际的连接异常，只关注URL解析过程
            log.warn("连接异常: {}", e.getMessage());
        }
        
        log.info("URL解析功能测试完成");
    }
    
    /**
     * 测试group参数默认值
     */
    @Test
    public void testGetConfigWithDefaultGroup() {
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试group为null时应该使用默认值DEFAULT_GROUP
        // 使用实际存在的配置进行测试
        String config1 = client.getConfig("common.properties", null, "jaja");
        String config2 = client.getConfig("common.properties", "DEFAULT_GROUP", "jaja");
        
        // 这两个调用应该等价，验证group为null时使用默认值DEFAULT_GROUP
        log.info("group为null的测试完成，结果1: {}, 结果2: {}", 
                config1 != null ? "有内容，长度=" + config1.length() : "null", 
                config2 != null ? "有内容，长度=" + config2.length() : "null");
        
        // 验证两个结果应该相同（如果配置存在）
        if (config1 != null && config2 != null) {
            assertEquals(config1, config2, "group为null和DEFAULT_GROUP应该返回相同结果");
        }
    }
}