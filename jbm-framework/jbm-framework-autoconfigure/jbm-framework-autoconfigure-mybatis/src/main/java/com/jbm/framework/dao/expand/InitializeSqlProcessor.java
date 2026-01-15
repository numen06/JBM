package com.jbm.framework.dao.expand;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.dao.SqlAutoExecuteProperties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Map;

/**
 * SQL自动执行处理器
 * 在MyBatis初始化完成后，自动扫描并执行 classpath:sql/schema/ 目录下的SQL文件
 * 使用 InitializingBean 接口，在 SqlSessionFactory 创建完成后立即初始化
 * 
 * 注意：此类需要在配置类中注册为Bean（在MybatisPlusConfig中），并使用 @DependsOn("sqlSessionFactory")
 * 
 * @author wesley
 */
@Slf4j
public class InitializeSqlProcessor implements InitializingBean {

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
    /**
     * -- SETTER --
     *  设置SqlSessionFactory（通过Bean后处理或直接注入）
     *  用于从MyBatis获取DataSource
     */
    @Setter
    private SqlSessionFactory sqlSessionFactory;
    private volatile boolean initialized = false;

    public InitializeSqlProcessor() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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

        if (applicationContext == null) {
            log.error("ApplicationContext 未设置，跳过SQL自动执行");
            return;
        }
        
        try {
            DataSource dataSource = getDataSource(applicationContext);
            if (dataSource == null) {
                log.error("获取数据源失败，跳过SQL自动执行。请检查数据源配置。");
                // 输出详细调试信息
                try {
                    Map<String, DataSource> allDataSources = applicationContext.getBeansOfType(DataSource.class);
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
            ResourcePatternResolver resourcePatternResolver = applicationContext;
            ResourceLoader resourceLoader = applicationContext;
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
     * 2. 从SqlSessionFactory获取DataSource（MyBatis使用的数据源，多数据源场景下为默认数据源）
     * 3. Spring容器中@Primary标注的DataSource（fallback）
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

        // 2. 从SqlSessionFactory获取DataSource（MyBatis使用的数据源）
        if (sqlSessionFactory != null) {
            try {
                DataSource dataSource = sqlSessionFactory.getConfiguration()
                        .getEnvironment()
                        .getDataSource();
                if (dataSource != null) {
                    log.debug("从SqlSessionFactory获取数据源");
                    return dataSource;
                }
            } catch (Exception e) {
                log.debug("从SqlSessionFactory获取数据源失败: {}", e.getMessage());
            }
        }

        // 3. 尝试获取@Primary标注的DataSource（fallback）
        try {
            DataSource primaryDataSource = context.getBean(DataSource.class);
            log.debug("使用Spring容器中的主数据源");
            return primaryDataSource;
        } catch (org.springframework.beans.factory.NoUniqueBeanDefinitionException e) {
            log.debug("发现多个DataSource Bean，但未找到@Primary标注的数据源");
        } catch (Exception e) {
            log.debug("未找到主数据源: {}", e.getMessage());
        }

        return null;
    }
}
