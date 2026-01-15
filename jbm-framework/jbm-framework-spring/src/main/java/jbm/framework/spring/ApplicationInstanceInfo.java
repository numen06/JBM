package jbm.framework.spring;

import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * 应用实例信息工具类
 * 用于获取应用名称、实例ID、主机名、端口等信息
 * 
 * @author wesley
 */
@Slf4j
public class ApplicationInstanceInfo {

    private static volatile String applicationName;
    private static volatile String instanceId;
    private static volatile String hostname;
    private static volatile String port;
    private static volatile String ip;
    
    /**
     * 获取应用名称
     */
    public static String getApplicationName() {
        if (applicationName == null) {
            synchronized (ApplicationInstanceInfo.class) {
                if (applicationName == null) {
                    try {
                        Environment env = SpringContextHolder.getApplicationContext().getEnvironment();
                        applicationName = env.getProperty("spring.application.name", "unknown-application");
                    } catch (Exception e) {
                        log.warn("获取应用名称失败，使用默认值", e);
                        applicationName = "unknown-application";
                    }
                }
            }
        }
        return applicationName;
    }
    
    /**
     * 获取实例ID（应用名称:端口:主机名）
     */
    public static String getInstanceId() {
        if (instanceId == null) {
            synchronized (ApplicationInstanceInfo.class) {
                if (instanceId == null) {
                    try {
                        String appName = getApplicationName();
                        String port = getPort();
                        String hostname = getHostname();
                        instanceId = String.format("%s:%s:%s", appName, port, hostname);
                    } catch (Exception e) {
                        log.warn("生成实例ID失败，使用UUID", e);
                        instanceId = UUID.randomUUID().toString();
                    }
                }
            }
        }
        return instanceId;
    }
    
    /**
     * 获取主机名
     */
    public static String getHostname() {
        if (hostname == null) {
            synchronized (ApplicationInstanceInfo.class) {
                if (hostname == null) {
                    try {
                        hostname = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        log.warn("获取主机名失败，使用默认值", e);
                        hostname = "unknown-host";
                    }
                }
            }
        }
        return hostname;
    }
    
    /**
     * 获取IP地址
     */
    public static String getIp() {
        if (ip == null) {
            synchronized (ApplicationInstanceInfo.class) {
                if (ip == null) {
                    try {
                        ip = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        log.warn("获取IP地址失败，使用默认值", e);
                        ip = "unknown-ip";
                    }
                }
            }
        }
        return ip;
    }
    
    /**
     * 获取端口号
     */
    public static String getPort() {
        if (port == null) {
            synchronized (ApplicationInstanceInfo.class) {
                if (port == null) {
                    try {
                        Environment env = SpringContextHolder.getApplicationContext().getEnvironment();
                        port = env.getProperty("server.port", "unknown-port");
                    } catch (Exception e) {
                        log.warn("获取端口号失败，使用默认值", e);
                        port = "unknown-port";
                    }
                }
            }
        }
        return port;
    }
    
    /**
     * 重置缓存（用于测试或重新加载配置）
     */
    public static void reset() {
        synchronized (ApplicationInstanceInfo.class) {
            applicationName = null;
            instanceId = null;
            hostname = null;
            port = null;
            ip = null;
        }
    }
}
