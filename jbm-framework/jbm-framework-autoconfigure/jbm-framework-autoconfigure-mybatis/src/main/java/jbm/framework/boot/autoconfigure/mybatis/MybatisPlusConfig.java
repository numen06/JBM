package jbm.framework.boot.autoconfigure.mybatis;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.google.common.collect.Sets;
import com.jbm.framework.dao.JdbcDataSourceProperties;
import com.jbm.framework.dao.mybatis.sqlInjector.CameHumpInterceptor;
import com.jbm.framework.dao.mybatis.sqlInjector.MasterDataSqlInjector;
import com.jbm.framework.dao.tenant.SpringTenantLineInnerInterceptor;
import com.jbm.framework.dao.tenant.TenantProperties;
import jbm.framework.boot.autoconfigure.mybatis.handler.MasterdataObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.Set;

/**
 * mybatis-plus配置类(下方注释有插件介绍)
 * PaginationInnerInterceptor 分页插件，自动识别数据库类型
 * https://baomidou.com/pages/97710a/
 * OptimisticLockerInnerInterceptor 乐观锁插件
 * https://baomidou.com/pages/0d93c0/
 * MetaObjectHandler 元对象字段填充控制器
 * https://baomidou.com/pages/4c6bcf/
 * ISqlInjector sql注入器
 * https://baomidou.com/pages/42ea4a/
 * BlockAttackInnerInterceptor 如果是对全表的删除或更新操作，就会终止该操作
 * https://baomidou.com/pages/f9a237/
 * IllegalSQLInnerInterceptor sql性能规范插件(垃圾SQL拦截)
 * IdentifierGenerator 自定义主键策略
 * https://baomidou.com/pages/568eb2/
 * TenantLineInnerInterceptor 多租户插件
 * https://baomidou.com/pages/aef2f2/
 * DynamicTableNameInnerInterceptor 动态表名插件
 * https://baomidou.com/pages/2a45ff/
 * @author wesley
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({JdbcDataSourceProperties.class,TenantProperties.class})
public class MybatisPlusConfig {


    @Autowired
    private MybatisPlusProperties mybatisPlusProperties;


    @Autowired
    private TenantProperties tenantProperties;

    @Autowired
    private ApplicationContext applicationContext;
    /**
     * 自动填充字段
     *
     * @return
     */
    @Bean
    public MasterdataObjectHandler masterdataObjectHandler() {
        return new MasterdataObjectHandler();
    }

    /**
     * 分页拦截器
     *
     * @return
     */

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        try {
            TenantLineHandler tenantLineHandler =  applicationContext.getBean(TenantLineHandler.class);
            if (ObjectUtil.isNotNull(tenantLineHandler)) {
                interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
                log.info("租户拦截器注入成功:{}", tenantLineHandler.getClass());
            }
        }catch (Exception e) {
            log.warn("没有找到租户拦截器");
        }
        return interceptor;
    }

    @Bean
    public MasterDataSqlInjector masterDataSqlInjector() {
        return new MasterDataSqlInjector();
    }

    @Bean
    public CameHumpInterceptor cameHumpInterceptor() {
        return new CameHumpInterceptor();
    }

    /**
     * 乐观锁插件
     */
//    public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
//        return new OptimisticLockerInnerInterceptor();
//    }

    /**
     * 自动刷新插件
     *
     * @return
     */
//    @ConditionalOnProperty("mybatis-plus.global-config.refresh")
    @Bean
    public MybatisMapperRefresh mybatisMapperRefresh(ApplicationContext applicationContext, SqlSessionFactory sqlSessionFactory) {
        Set<Resource> mapperLocations = Sets.newLinkedHashSet();
        for (String xx : mybatisPlusProperties.getMapperLocations()) {
            try {
                mapperLocations.addAll(Arrays.asList(applicationContext.getResources(xx)));
            } catch (Exception e) {
                continue;
            }
        }
        MybatisMapperRefresh mybatisMapperRefresh = new MybatisMapperRefresh(ArrayUtil.toArray(mapperLocations.iterator(), Resource.class), sqlSessionFactory, 10, 5,
                true);

        return mybatisMapperRefresh;
    }



}
