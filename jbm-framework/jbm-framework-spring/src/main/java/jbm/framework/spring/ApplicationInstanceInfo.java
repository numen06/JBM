package jbm.framework.spring;

import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * 应用实例信息工具类
 * 用于获取应用名称、实例ID、主机名、端口等信息
 * 通过 Spring 事件监听确保在 Spring 环境就绪后正确初始化
 * 
 * @author wesley
 */
@Slf4j
public class ApplicationInstanceInfo {

    // 缓存的应用信息（静态变量，单例模式）
    private static volatile String applicationName;
    private static volatile String instanceId;
    private static volatile String hostname;
    private static volatile String port;
    private static volatile String ip;
    
    /**
     * 安全地获取 ApplicationContext，如果未初始化则返回 null
     * 
     * @return ApplicationContext 或 null
     */
    private static ApplicationContext getApplicationContextSafely() {
        try {
            return SpringContextHolder.getApplicationContext();
        } catch (IllegalStateException e) {
            // ApplicationContext 未初始化，返回 null
            return null;
        } catch (Exception e) {
            // 其他异常也返回 null
            log.debug("获取 ApplicationContext 失败", e);
            return null;
        }
    }
    
    /**
     * 获取应用名称
     */
    public static String getApplicationName() {
        if (applicationName == null) {
            synchronized (ApplicationInstanceInfo.class) {
                if (applicationName == null) {
                    try {
                        ApplicationContext context = getApplicationContextSafely();
                        if (context != null) {
                            Environment env = context.getEnvironment();
                            applicationName = env.getProperty("spring.application.name", "unknown-application");
                        } else {
                            log.debug("ApplicationContext 未初始化，使用默认应用名称");
                            applicationName = "unknown-application";
                        }
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
                        ApplicationContext context = getApplicationContextSafely();
                        if (context != null) {
                            Environment env = context.getEnvironment();
                            port = env.getProperty("server.port", "unknown-port");
                        } else {
                            log.debug("ApplicationContext 未初始化，使用默认端口号");
                            port = "unknown-port";
                        }
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
    
    /**
     * 初始化应用信息（由 Spring 事件监听器调用，确保在 Spring 环境就绪后初始化）
     */
    public static void initialize() {
        // 预加载所有信息，确保后续调用能够获取到正确的值
        getApplicationName();
        getPort();
        getHostname();
        getIp();
        getInstanceId();
    }
    
    /**
     * 使用指定的 ApplicationContext 初始化应用信息
     * 用于在 SpringContextHolder 还未注入时直接使用事件中的 ApplicationContext
     * 
     * @param context ApplicationContext
     */
    public static void initializeWithContext(ApplicationContext context) {
        if (context == null) {
            log.warn("ApplicationContext 为 null，无法初始化");
            return;
        }
        
        synchronized (ApplicationInstanceInfo.class) {
            try {
                Environment env = context.getEnvironment();
                
                // 直接设置应用名称和端口
                applicationName = env.getProperty("spring.application.name", "unknown-application");
                port = env.getProperty("server.port", "unknown-port");
                
                // 获取主机名（如果还未初始化）
                if (hostname == null) {
                    try {
                        hostname = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        log.warn("获取主机名失败，使用默认值", e);
                        hostname = "unknown-host";
                    }
                }
                
                // 获取IP（如果还未初始化）
                if (ip == null) {
                    try {
                        ip = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        log.warn("获取IP地址失败，使用默认值", e);
                        ip = "unknown-ip";
                    }
                }
                
                // 生成实例ID
                instanceId = String.format("%s:%s:%s", applicationName, port, hostname);
                
                log.debug("使用 ApplicationContext 直接初始化完成 - 应用名称: {}, 端口: {}", applicationName, port);
            } catch (Exception e) {
                log.warn("使用 ApplicationContext 初始化失败", e);
            }
        }
    }
}
