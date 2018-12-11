package jbm.framework.beetl.core;

import org.beetl.core.Configuration;
import org.beetl.core.ErrorHandler;
import org.beetl.core.GroupTemplate;
import org.beetl.core.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * GroupTemplate工厂Bean<br>
 * 这个类不一定在Spring Web环境中使用，一般的Spring应用程序也能使用
 *
 * @author wesley.zhang
 */
public class GroupTemplateFactaryBean extends AbstractGroupTemplateConfig implements FactoryBean<GroupTemplate>, DisposableBean, BeanNameAware, ApplicationContextAware {
    // 日志
    private static final Logger LOG = LoggerFactory.getLogger(GroupTemplateFactaryBean.class);
    /**
     * beetl默认配置文件路径
     */
    private static final String BEETL_DEFAULT_CONFIG_RESOURCE_NAME = "classpath:org/beetl/core/beetl-default.properties";

    /* ----- ----- ----- ----- 其他方法 ----- ----- ----- ----- */
    /**
     * 这个GroupTemplate的BeanName
     */
    private String beanName = null;
    /**
     * Spring应用程序上下文
     */
    private ApplicationContext applicationContext = null;

    /**
     * Spring应用程序上下文
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 这个GroupTemplate的BeanName
     *
     * @param beanName
     */
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * 这个GroupTemplateFactoryBean创建的groupTemplate实例
     */
    private GroupTemplate groupTemplate = null;

    /**
     * 配置属性
     */
    private Properties configProperties = null;
    /**
     * 配置文件地址
     */
    private Resource configFileResource = null;
    /**
     * Beetl资源加载器，如果未指定，会自动依据ApplicationContext和配置文件识别
     */
    private ResourceLoader resourceLoader = null;
    /**
     * 异常处理器
     */
    private ErrorHandler errorHandler = null;
    /**
     * 共享变量
     */
    private Map<String, Object> sharedVars = null;

    /**
     * 配置扩展配置对象
     */
    private List<GroupTemplateConfig> extGroupTemplateConfigs = null;

    /**
     * 配置属性
     *
     * @param configProperties
     */
    public void setConfigProperties(Properties configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * 配置文件地址
     *
     * @param configFileResource
     */
    public void setConfigFileResource(Resource configFileResource) {
        this.configFileResource = configFileResource;
    }

    /**
     * Beetl资源加载器，如果未指定，会自动依据ApplicationContext和配置文件识别
     *
     * @param resourceLoader
     */
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 异常处理器
     *
     * @param errorHandler
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * 共享参数
     *
     * @param sharedVars
     */
    public void setSharedVars(Map<String, Object> sharedVars) {
        this.sharedVars = sharedVars;
    }

    /**
     * 配置扩展配置对象
     *
     * @param extGroupTemplateConfigs
     */
    public void setExtGroupTemplateConfigs(List<GroupTemplateConfig> extGroupTemplateConfigs) {
        this.extGroupTemplateConfigs = extGroupTemplateConfigs;
    }

    /* ----- ----- ----- ----- 其他方法 ----- ----- ----- ----- */

    /**
     * 创建GroupTemplate对象
     *
     * @return
     * @throws IOException 配置文件数据加载IO异常
     */
    @Override
    public GroupTemplate getObject() throws IOException {
        initGroupTemplate();
        // GroupTemplate 配置
        // 设置异常处理器
        if (errorHandler != null) {
            groupTemplate.setErrorHandler(errorHandler);
        }

        // 设置共享变量
        if (sharedVars != null) {
            groupTemplate.setSharedVars(sharedVars);
        }

        // 进行扩展配置
        if (extGroupTemplateConfigs == null) {
            Collection<GroupTemplateConfig> tempExtGroupTemplateConfigs = applicationContext.getBeansOfType(GroupTemplateConfig.class).values();
            extGroupTemplateConfigs = new ArrayList<GroupTemplateConfig>(tempExtGroupTemplateConfigs);
        }
        if (!extGroupTemplateConfigs.isEmpty()) {
            for (GroupTemplateConfig extGroupTemplateConfig : extGroupTemplateConfigs) {
                extGroupTemplateConfig.config(groupTemplate);
            }
        }

        config(groupTemplate);
        return groupTemplate;
    }

    /**
     * 初始化GroupTemplate对象
     *
     * @return
     * @throws IOException
     */
    private void initGroupTemplate() throws IOException {
        // 配置数据加载
        Configuration configuration = null;
        // 如果都未设置，取默认的配置
        if ((configProperties == null) && (configFileResource == null)) {
            configuration = Configuration.defaultConfiguration();
        } else { // 否则采用Properties的形式加载
            Properties properties = new Properties();
            InputStream in = null;
            try {
                // 如果指定了配置文件，先加载配置文件
                if (configFileResource != null) {
                    in = configFileResource.getInputStream();
                } else {
                    // 取Beetl默认配置
                    in = applicationContext.getResource(BEETL_DEFAULT_CONFIG_RESOURCE_NAME).getInputStream();
                }
                properties.load(in);
            } finally {
                if (in != null) {
                    in.close();
                    in = null;
                }
            }
            // 如果指定了configProperties，对已加载的内容进行替换
            if (configProperties != null) {
                for (Enumeration<?> keys = configProperties.propertyNames(); keys.hasMoreElements(); ) {
                    String key = (String) keys.nextElement();
                    String value = configProperties.getProperty(key);
                    properties.setProperty(key, value);
                }
            }
            // 使用配置项配置properties
            configuration = new Configuration(properties);
        }

        // 如果未指定，返回
        if (resourceLoader != null) {
            groupTemplate = new GroupTemplate(resourceLoader, configuration);
        } else {
            groupTemplate = new GroupTemplate(configuration);
        }
    }

    @Override
    public Class<GroupTemplate> getObjectType() {
        return GroupTemplate.class;
    }

    /**
     * 生成的GroupTemplate在Spring单例管理
     *
     * @return
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        groupTemplate.close();

        LOG.info("GroupTemplate name \"" + beanName + "\" closed.");
    }
}
