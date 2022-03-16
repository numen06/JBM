package jbm.framework.boot.autoconfigure.mybatis;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.google.common.collect.Sets;
import com.jbm.framework.dao.mybatis.sqlInjector.CameHumpInterceptor;
import com.jbm.framework.dao.mybatis.sqlInjector.MasterDataSqlInjector;
import jbm.framework.boot.autoconfigure.mybatis.handler.MasterdataObjectHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class MybatisPlusConfig {


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
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        paginationInterceptor.setDialectType("mysql");
        List<ISqlParser> sqlParserList = new ArrayList<>();
//		TenantSqlParser tenantSqlParser = new TenantSqlParser();
//		tenantSqlParser.setTenantHandler(new TenantHandler() {
//			@Override
//			public Expression getTenantId() {
//				return new LongValue(1L);
//			}
//
//			@Override
//			public String getTenantIdColumn() {
//				return "tenant_id";
//			}
//
//			@Override
//			public boolean doTableFilter(String tableName) {
//				// 这里可以判断是否过滤表
//				/*
//				 * if ("user".equals(tableName)) { return true; }
//				 */
//				return false;
//			}
//		});
        paginationInterceptor.setSqlParserList(sqlParserList);
//      paginationInterceptor.setSqlParserFilter(new ISqlParserFilter() {
//      @Override
//      public boolean doFilter(MetaObject metaObject) {
//          MappedStatement ms = PluginUtils.getMappedStatement(metaObject);
//          // 过滤自定义查询此时无租户信息约束出现
//          if ("com.baomidou.springboot.mapper.UserMapper.selectListBySQL".equals(ms.getId())) {
//              return true;
//          }
//          return false;
//      }
//  });
        return paginationInterceptor;
    }

    @Bean
    public MasterDataSqlInjector masterDataSqlInjector() {
        return new MasterDataSqlInjector();
    }


    @Bean
    public CameHumpInterceptor cameHumpInterceptor() {
        return new CameHumpInterceptor();
    }

    @Autowired
    private MybatisPlusProperties mybatisPlusProperties;

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
