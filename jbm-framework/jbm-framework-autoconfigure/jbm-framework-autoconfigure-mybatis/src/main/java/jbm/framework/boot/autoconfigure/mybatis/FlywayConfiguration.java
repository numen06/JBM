package jbm.framework.boot.autoconfigure.mybatis;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.dao.SqlAutoExecuteProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private SqlSessionFactory sqlSessionFactory;

    @Autowired(required = false)
    private SqlAutoExecuteProperties sqlAutoExecuteProperties;
    
    @Autowired
    private FlywayProperties flywayProperties;

    /**
     * 配置Flyway Bean，覆盖Spring Boot自动配置的Flyway
     * 复用配置文件中的所有Flyway设置，只替换数据源为MyBatis使用的数据源
     * 
     * Spring Boot的FlywayAutoConfiguration会自动读取spring.flyway.*配置
     * 我们只需要提供一个自定义的Flyway Bean，使用正确的数据源即可
     * 
     * 如果检测到DynamicRoutingDataSource，会为每个实际数据源分别创建Flyway实例
     * 使用initMethod = "migrate"确保Bean创建后自动执行迁移
     */
    @Bean(initMethod = "migrate")
    @Primary
    @DependsOn("sqlSessionFactory")
    public Flyway flyway() {
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

        // 检测是否是DynamicRoutingDataSource
        Map<String, DataSource> resolvedDataSources = resolveDataSources(dataSource);
        
        if (resolvedDataSources.size() > 1) {
            // 多数据源场景：为每个数据源分别创建Flyway并执行迁移
            log.info("检测到多数据源场景，共 {} 个数据源，将分别为每个数据源执行Flyway迁移", resolvedDataSources.size());
            Flyway primaryFlyway = null;
            for (Map.Entry<String, DataSource> entry : resolvedDataSources.entrySet()) {
                String dsKey = entry.getKey();
                DataSource ds = entry.getValue();
                log.info("========== 为数据源 [{}] 初始化Flyway ==========", dsKey);
                Flyway flyway = createFlywayForDataSource(ds, dsKey);
                if (primaryFlyway == null) {
                    primaryFlyway = flyway; // 返回第一个作为主Flyway Bean
                }
            }
            log.info("========== 所有数据源的Flyway初始化完成 ==========");
            return primaryFlyway != null ? primaryFlyway : createFlywayForDataSource(dataSource, "default");
        } else {
            // 单数据源场景
            return createFlywayForDataSource(dataSource, "default");
        }
    }
    
    /**
     * 解析数据源，如果是DynamicRoutingDataSource，则获取其内部的所有数据源
     * 
     * @param dataSource 数据源
     * @return 数据源Map，key为数据源标识，value为数据源实例
     */
    private Map<String, DataSource> resolveDataSources(DataSource dataSource) {
        Map<String, DataSource> result = new LinkedHashMap<>();
        
        // 检查是否是DynamicRoutingDataSource类型
        String className = dataSource.getClass().getName();
        if (className.contains("DynamicRoutingDataSource") || className.contains("DynamicDataSource")) {
            try {
                // 尝试通过反射获取targetDataSources或resolvedDataSources
                Method[] methods = dataSource.getClass().getMethods();
                Method targetDataSourcesMethod = null;
                Method resolvedDataSourcesMethod = null;
                
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.equals("getTargetDataSources") || methodName.equals("getResolvedDataSources")) {
                        if (method.getParameterCount() == 0) {
                            Class<?> returnType = method.getReturnType();
                            if (Map.class.isAssignableFrom(returnType)) {
                                if (methodName.contains("Resolved")) {
                                    resolvedDataSourcesMethod = method;
                                } else {
                                    targetDataSourcesMethod = method;
                                }
                            }
                        }
                    }
                }
                
                // 优先使用resolvedDataSources，其次使用targetDataSources
                Method methodToUse = resolvedDataSourcesMethod != null ? resolvedDataSourcesMethod : targetDataSourcesMethod;
                if (methodToUse != null) {
                    methodToUse.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<Object, DataSource> targetDataSources = (Map<Object, DataSource>) methodToUse.invoke(dataSource);
                    if (targetDataSources != null && !targetDataSources.isEmpty()) {
                        for (Map.Entry<Object, DataSource> entry : targetDataSources.entrySet()) {
                            String key = entry.getKey() != null ? entry.getKey().toString() : "unknown";
                            result.put(key, entry.getValue());
                            log.info("解析到数据源: {} -> {}", key, entry.getValue().getClass().getSimpleName());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("无法从DynamicRoutingDataSource中解析数据源，将使用原始数据源: {}", e.getMessage());
            }
        }
        
        // 如果解析失败或不是DynamicRoutingDataSource，返回原始数据源
        if (result.isEmpty()) {
            result.put("default", dataSource);
        }
        
        return result;
    }
    
    /**
     * 为指定数据源创建Flyway实例
     * 
     * @param dataSource 数据源
     * @param dataSourceKey 数据源标识
     * @return Flyway实例
     */
    private Flyway createFlywayForDataSource(DataSource dataSource, String dataSourceKey) {
        // 使用FlywayProperties中的配置
        org.flywaydb.core.api.configuration.FluentConfiguration flywayConfig = Flyway.configure()
                .dataSource(dataSource);

//        flywayConfig.initSql("SET @__flyway_session_var__ = 'workaround_for_tidb';");

        // 复用配置文件中的所有设置
        List<String> locations = buildLocations(dataSource, dataSourceKey);
        Charset encoding = flywayProperties.getEncoding();
        boolean baselineOnMigrate = flywayProperties.isBaselineOnMigrate();
        boolean validateOnMigrate = flywayProperties.isValidateOnMigrate();
        boolean cleanDisabled = flywayProperties.isCleanDisabled();

        // 应用配置
        if (!locations.isEmpty()) {
            flywayConfig.locations(locations.toArray(new String[0]));
            log.info("数据源 [{}] Flyway locations: {}", dataSourceKey, locations);
        } else {
            flywayConfig.locations("classpath:db/migration/");
            log.info("数据源 [{}] Flyway locations: classpath:db/migration/ (默认)", dataSourceKey);
        }
        if (encoding != null) {
            flywayConfig.encoding(encoding);
        }
        flywayConfig.baselineOnMigrate(baselineOnMigrate);
        flywayConfig.validateOnMigrate(validateOnMigrate);
        flywayConfig.cleanDisabled(cleanDisabled);


        Flyway flyway = flywayConfig.load();
        log.info("数据源 [{}] Flyway Bean创建完成，将在初始化时执行迁移", dataSourceKey);
        return flyway;
    }

    /**
     * 构建Flyway的locations配置
     * 规则：
     * 1. 如果是单数据源或主数据源，使用spring.flyway.locations配置（默认：db/migration/）
     * 2. 如果是多数据源的非主数据源，动态指定到classpath:{db}/migration/目录
     * 
     * @param dataSource 当前使用的数据源
     * @param dataSourceKey 数据源标识
     * @return locations列表
     */
    private List<String> buildLocations(DataSource dataSource, String dataSourceKey) {
        // 获取所有数据源
        Map<String, DataSource> allDataSources = applicationContext.getBeansOfType(DataSource.class);
        boolean isMultiDataSource = allDataSources.size() > 1;
        
        // 获取基础配置
        List<String> baseLocations = flywayProperties.getLocations();
        
        // 判断是否是主数据源（@Primary标注的数据源）
        boolean isPrimaryDataSource = isPrimaryDataSource(dataSource, allDataSources);
        
        // 如果提供了dataSourceKey且不是"default"，说明是多数据源场景
        if (isMultiDataSource && !isPrimaryDataSource && StrUtil.isNotBlank(dataSourceKey) && !"default".equals(dataSourceKey)) {
            // 多数据源的非主数据源场景：使用动态目录 classpath:{db}/migration/
            String dynamicLocation = "classpath:" + dataSourceKey + "/migration/";
            log.info("数据源 [{}] 多数据源非主数据源场景，使用动态目录: {}", dataSourceKey, dynamicLocation);
            return java.util.Arrays.asList(dynamicLocation);
        }
        
        // 单数据源或主数据源：使用spring.flyway.locations配置
        if (baseLocations != null && !baseLocations.isEmpty()) {
            log.info("数据源 [{}] 单数据源或主数据源场景，使用配置的locations: {}", dataSourceKey, baseLocations);
            return baseLocations;
        } else {
            log.info("数据源 [{}] 单数据源或主数据源场景，使用默认locations: classpath:db/migration/", dataSourceKey);
            return Collections.singletonList("classpath:db/migration/");
        }
    }
    
    /**
     * 判断是否是主数据源（@Primary标注的数据源）
     * 
     * @param dataSource 当前数据源
     * @param allDataSources 所有数据源Map
     * @return 是否是主数据源
     */
    private boolean isPrimaryDataSource(DataSource dataSource, Map<String, DataSource> allDataSources) {
        try {
            // 尝试获取@Primary标注的数据源
            DataSource primaryDataSource = applicationContext.getBean(DataSource.class);
            // 如果当前数据源就是主数据源，返回true
            return primaryDataSource == dataSource;
        } catch (org.springframework.beans.factory.NoUniqueBeanDefinitionException e) {
            // 如果有多个数据源但没有@Primary，返回false
            return false;
        } catch (Exception e) {
            // 其他异常，返回false
            return false;
        }
    }
    
    /**
     * 获取数据源标识
     * 优先级：
     * 1. 配置中指定的datasourceBeanName
     * 2. 从ApplicationContext中找到对应的Bean名称
     * 3. 如果找不到，返回null（使用默认配置）
     * 
     * @param dataSource 当前数据源
     * @param allDataSources 所有数据源Map
     * @return 数据源标识
     */
    private String getDataSourceIdentifier(DataSource dataSource, Map<String, DataSource> allDataSources) {
        // 1. 如果配置了datasourceBeanName，直接使用
        if (sqlAutoExecuteProperties != null && StrUtil.isNotBlank(sqlAutoExecuteProperties.getDatasourceBeanName())) {
            String beanName = sqlAutoExecuteProperties.getDatasourceBeanName();
            log.debug("使用配置的数据源标识: {}", beanName);
            return beanName;
        }
        
        // 2. 从ApplicationContext中找到对应的Bean名称
        for (Map.Entry<String, DataSource> entry : allDataSources.entrySet()) {
            if (entry.getValue() == dataSource) {
                String beanName = entry.getKey();
                log.debug("从Bean名称获取数据源标识: {}", beanName);
                return beanName;
            }
        }
        
        // 3. 如果找不到，返回null（将使用默认配置）
        log.debug("未找到数据源标识，将使用默认配置");
        return null;
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
