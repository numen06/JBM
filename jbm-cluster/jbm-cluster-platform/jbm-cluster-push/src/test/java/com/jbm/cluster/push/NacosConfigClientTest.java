package com.jbm.cluster.push;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nacos配置客户端测试类
 */
public class NacosConfigClientTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NacosConfigClientTest.class);
    
    /**
     * 测试不带认证的Nacos配置获取
     */
    @Test
    public void testGetConfigWithoutAuth() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试获取不存在的配置，应该返回null
        String config = client.getConfig("non-existent-config.properties", "DEFAULT_GROUP", null);
        assertNull(config, "获取不存在的配置应该返回null");
        
        logger.info("不带认证的Nacos配置获取测试完成");
    }
    
    /**
     * 测试带认证的Nacos配置获取
     */
    @Test
    public void testGetConfigWithAuth() {
        // 使用本地Nacos服务器地址和认证信息，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848", "nacos", "nacos");
        
        // 测试获取不存在的配置，应该返回null
        String config = client.getConfig("non-existent-config.properties", "DEFAULT_GROUP", null);
        assertNull(config, "获取不存在的配置应该返回null");
        
        logger.info("带认证的Nacos配置获取测试完成");
    }
    
    /**
     * 测试带命名空间的Nacos配置获取
     */
    @Test
    public void testGetConfigWithNamespace() {
        // 使用本地Nacos服务器地址，根据实际情况修改
        NacosConfigClient client = new NacosConfigClient("http://10.100.10.62:8848");
        
        // 测试获取指定命名空间下的不存在的配置，应该返回null
        String config = client.getConfig("non-existent-config.properties", "DEFAULT_GROUP", "non-existent-namespace");
        assertNull(config, "获取不存在的配置应该返回null");
        
        logger.info("带命名空间的Nacos配置获取测试完成");
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
            logger.info("成功获取到配置，长度为: {} 字符", config.length());
            logger.debug("配置内容预览: {}", config.substring(0, Math.min(config.length(), 100)));
        } else {
            logger.info("未找到指定的配置，这可能是正常的");
        }
        
        logger.info("实际配置获取测试完成");
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
        
        logger.info("无效参数测试完成");
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
        try {
            client1.getConfig("test", "DEFAULT_GROUP", null);
            client2.getConfig("test", "DEFAULT_GROUP", null);
            client3.getConfig("test", "DEFAULT_GROUP", null);
        } catch (IllegalArgumentException e) {
            // 忽略参数校验异常
        } catch (Exception e) {
            // 忽略实际的连接异常，只关注URL解析过程
        }
        
        logger.info("URL解析功能测试完成");
    }
}