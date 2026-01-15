package com.jbm.framework.dao.expand;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.jbm.framework.dao.JdbcDataSourceProperties;
import com.jbm.framework.dao.SqlAutoExecuteProperties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Map;

/**
 * SQL自动执行处理器
 * 在应用完全就绪后，自动扫描并执行 classpath:sql/schema/ 目录下的SQL文件
 * 使用 ApplicationReadyEvent 确保数据源已完全初始化
 * 
 * 注意：此类需要在配置类中注册为Bean（在MybatisPlusConfig中）
 * 
 * @author wesley
 */
@Slf4j
@Order
public class InitializeSqlProcessor implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * -- SETTER --
     *  设置配置属性（通过Bean后处理或直接注入）
     */
    @Setter
    private SqlAutoExecuteProperties sqlAutoExecuteProperties;
    /**
     * -- SETTER --
     *  设置ApplicationContext（通过Bean后处理或直接注入）
     */
    @Setter
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    public InitializeSqlProcessor() {
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("========== InitializeSqlProcessor 初始化开始 ==========");
        
        // 避免重复执行
        if (initialized) {
            log.warn("已初始化，跳过执行");
            return;
        }
        
        // 检查是否启用
        if (sqlAutoExecuteProperties != null && !sqlAutoExecuteProperties.getEnabled()) {
            log.info("SQL自动执行功能已禁用，跳过扫描");
            return;
        }

        ApplicationContext context = event.getApplicationContext();
        
        try {
            DataSource dataSource = getDataSource(context);
            if (dataSource == null) {
                log.error("获取数据源失败，跳过SQL自动执行。请检查数据源配置。");
                // 输出详细调试信息
                try {
                    Map<String, DataSource> allDataSources = context.getBeansOfType(DataSource.class);
                    if (allDataSources.isEmpty()) {
                        log.error("容器中没有找到任何DataSource Bean！");
                    } else {
                        log.error("容器中所有的DataSource Bean: {}", allDataSources.keySet());
                    }
                } catch (Exception e) {
                    log.error("无法获取DataSource Bean列表", e);
                }
                return;
            }
            log.info("数据源获取成功: {}", dataSource.getClass().getSimpleName());

            // 获取Spring的资源解析器（ResourcePatternResolver通常就是ApplicationContext）
            ResourcePatternResolver resourcePatternResolver = context;
            ResourceLoader resourceLoader = context;
            SqlPrepareRunner sqlPrepareRunner = new SqlPrepareRunner(dataSource, sqlAutoExecuteProperties, 
                                                                      resourcePatternResolver, resourceLoader);
            sqlPrepareRunner.scanSqlFiles();
            
            initialized = true;
            log.info("========== InitializeSqlProcessor 初始化完成 ==========");
        } catch (Exception e) {
            log.error("初始化数据库文件扫描失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 获取数据源
     * 优先级：
     * 1. 配置指定的数据源Bean名称
     * 2. Spring容器中@Primary标注的DataSource
     * 3. 唯一的DataSource Bean
     * 4. 从配置属性创建SimpleDataSource
     */
    private DataSource getDataSource(ApplicationContext context) {
        // 1. 如果配置了指定的数据源Bean名称，优先使用
        if (sqlAutoExecuteProperties != null && StrUtil.isNotBlank(sqlAutoExecuteProperties.getDatasourceBeanName())) {
            try {
                DataSource ds = context.getBean(sqlAutoExecuteProperties.getDatasourceBeanName(), DataSource.class);
                log.debug("使用配置指定的数据源: {}", sqlAutoExecuteProperties.getDatasourceBeanName());
                return ds;
            } catch (Exception e) {
                log.debug("未找到配置指定的数据源Bean: {}, 尝试其他方式", sqlAutoExecuteProperties.getDatasourceBeanName());
            }
        }

        // 2. 尝试获取@Primary标注的DataSource
        try {
            DataSource primaryDataSource = context.getBean(DataSource.class);
            log.debug("使用Spring容器中的主数据源");
            return primaryDataSource;
        } catch (org.springframework.beans.factory.NoUniqueBeanDefinitionException e) {
            log.debug("发现多个DataSource Bean，尝试查找所有DataSource Bean");
        } catch (Exception e) {
            log.debug("未找到主数据源，尝试查找所有DataSource Bean");
        }

        // 3. 查找所有DataSource Bean，如果只有一个则使用
        try {
            Map<String, DataSource> dataSourceMap = context.getBeansOfType(DataSource.class);
            if (dataSourceMap.size() == 1) {
                String beanName = dataSourceMap.keySet().iterator().next();
                log.debug("使用唯一的DataSource Bean: {}", beanName);
                return dataSourceMap.values().iterator().next();
            } else if (dataSourceMap.size() > 1) {
                log.warn("发现多个DataSource Bean: {}, 使用第一个", dataSourceMap.keySet());
                return dataSourceMap.values().iterator().next();
            }
        } catch (Exception e) {
            log.debug("查找DataSource Bean失败: {}", e.getMessage());
        }

        // 4. 从配置属性创建SimpleDataSource
        try {
            JdbcDataSourceProperties dataSourceProperties = context.getBean(JdbcDataSourceProperties.class);
            if (dataSourceProperties != null && StrUtil.isNotBlank(dataSourceProperties.getUrl())) {
                log.debug("从配置属性创建数据源");
                return new SimpleDataSource(
                    dataSourceProperties.getUrl(),
                    dataSourceProperties.getUsername(),
                    dataSourceProperties.getPassword()
                );
            }
        } catch (Exception e) {
            log.debug("从配置属性创建数据源失败: {}", e.getMessage());
        }

        return null;
    }
}
