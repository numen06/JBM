package jbm.framework.boot.autoconfigure.mybatis;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.dao.SqlAutoExecuteProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Flyway数据库迁移配置类
 * 替换原有的InitializeSqlProcessor功能，使用Flyway进行SQL文件的版本化管理和自动执行
 * 
 * 直接使用Spring Boot的Flyway自动配置（读取mybatis-plus.properties中的spring.flyway.*配置）
 * 只覆盖数据源部分，确保使用MyBatis配置的数据源
 * 
 * 迁移历史处理说明：
 * 1. 对于新数据库：Flyway会自动创建flyway_schema_history表并执行所有迁移脚本
 * 2. 对于已有数据库：由于配置了baseline-on-migrate=true，Flyway会自动创建baseline并执行未执行的迁移脚本
 * 3. 如果数据库中已有sql_initialize表的记录，需要手动迁移到flyway_schema_history表，或使用flyway baseline命令
 * 
 * @author wesley
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "jbm.sql.auto-execute", name = "enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.boot.autoconfigure.condition.ConditionalOnClass(Flyway.class)
public class FlywayConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private SqlSessionFactory sqlSessionFactory;

    @Autowired(required = false)
    private SqlAutoExecuteProperties sqlAutoExecuteProperties;

    /**
     * 配置Flyway Bean，覆盖Spring Boot自动配置的Flyway
     * 复用配置文件中的所有Flyway设置，只替换数据源为MyBatis使用的数据源
     * 
     * Spring Boot的FlywayAutoConfiguration会自动读取spring.flyway.*配置
     * 我们只需要提供一个自定义的Flyway Bean，使用正确的数据源即可
     * 
     * 使用initMethod = "migrate"确保Bean创建后自动执行迁移
     */
    @Bean(initMethod = "migrate")
    @Primary
    @DependsOn("sqlSessionFactory")
    public Flyway flyway(org.springframework.boot.autoconfigure.flyway.FlywayProperties flywayProperties) {
        log.info("========== Flyway 初始化开始 ==========");
        
        DataSource dataSource = getDataSource();
        if (dataSource == null) {
            log.error("获取数据源失败，Flyway初始化被跳过。请检查数据源配置。");
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
            throw new IllegalStateException("无法获取数据源，Flyway初始化失败");
        }
        
        log.info("数据源获取成功: {}", dataSource.getClass().getSimpleName());

        // 使用Spring Boot的FlywayProperties配置，只替换数据源
        org.flywaydb.core.api.configuration.FluentConfiguration flywayConfig = Flyway.configure()
                .dataSource(dataSource);  // 使用MyBatis的数据源

        // 复用配置文件中的所有设置
        if (flywayProperties.getLocations() != null && !flywayProperties.getLocations().isEmpty()) {
            flywayConfig.locations(flywayProperties.getLocations().toArray(new String[0]));
            log.info("Flyway locations: {}", flywayProperties.getLocations());
        } else {
            flywayConfig.locations("classpath:sql/schema");
            log.info("Flyway locations: classpath:sql/schema (默认)");
        }
        if (flywayProperties.getEncoding() != null) {
            flywayConfig.encoding(flywayProperties.getEncoding());
        }
        flywayConfig.baselineOnMigrate(flywayProperties.isBaselineOnMigrate());
        flywayConfig.validateOnMigrate(flywayProperties.isValidateOnMigrate());
        flywayConfig.cleanDisabled(flywayProperties.isCleanDisabled());

        Flyway flyway = flywayConfig.load();

        log.info("========== Flyway Bean创建完成，将在初始化时执行迁移 ==========");
        return flyway;
    }

    /**
     * 获取数据源
     * 优先级：
     * 1. 配置指定的数据源Bean名称
     * 2. 从SqlSessionFactory获取DataSource（MyBatis使用的数据源，多数据源场景下为默认数据源）
     * 3. Spring容器中@Primary标注的DataSource（fallback）
     */
    private DataSource getDataSource() {
        // 1. 如果配置了指定的数据源Bean名称，优先使用
        if (sqlAutoExecuteProperties != null && StrUtil.isNotBlank(sqlAutoExecuteProperties.getDatasourceBeanName())) {
            try {
                DataSource ds = applicationContext.getBean(sqlAutoExecuteProperties.getDatasourceBeanName(), DataSource.class);
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
            DataSource primaryDataSource = applicationContext.getBean(DataSource.class);
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
