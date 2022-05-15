package com.jbm.cluster.platform.gateway.service.impl;

import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.cluster.platform.gateway.config.JdbcDataSourceProperties;
import com.jbm.cluster.platform.gateway.service.RouteDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * @Created wesley.zhang
 * @Date 2022/5/10 19:12
 * @Description TODO
 */
@Slf4j
public class JdbcRouteDataSource implements RouteDataSource {

    private final static String SELECT_ROUTES = "SELECT * FROM gateway_route WHERE status = 1";

    private final static String SELECT_LIMIT_PATH = "SELECT\n" +
            "        i.policy_id,\n" +
            "        p.limit_quota,\n" +
            "        p.interval_unit,\n" +
            "        p.policy_name,\n" +
            "        a.api_id,\n" +
            "        a.api_code,\n" +
            "        a.api_name,\n" +
            "        a.api_category,\n" +
            "        a.service_id,\n" +
            "        a.path,\n" +
            "        r.url\n" +
            "    FROM\n" +
            "        gateway_rate_limit_api AS i\n" +
            "    INNER JOIN gateway_rate_limit AS p ON i.policy_id = p.policy_id\n" +
            "    INNER JOIN base_api AS a ON i.api_id = a.api_id\n" +
            "    INNER JOIN gateway_route AS r ON a.servic" +
            "e_id = r.route_name\n" +
            "    WHERE\n" +
            "        p.policy_type = 'url'";

    private DataSource ds;

    public JdbcRouteDataSource(JdbcDataSourceProperties dataSourceProperties) {
        this.ds = new SimpleDataSource(dataSourceProperties.getUrl(), dataSourceProperties.getUsername(), dataSourceProperties.getPassword());
    }


    @Override
    public List<GatewayRoute> getRouteList() {
        List<GatewayRoute> routeList = Lists.newArrayList();
        //获取默认数据源
        Connection conn = null;
        try {
            conn = ds.getConnection();
            /* 执行查询语句，返回实体列表，一个Entity对象表示一行的数据，Entity对象是一个继承自HashMap的对象，存储的key为字段名，value为字段值 */
            List<Entity> entityList = SqlExecutor.query(conn, SELECT_ROUTES,
                    new EntityListHandler());
            entityList.forEach(new Consumer<Entity>() {
                @Override
                public void accept(Entity entity) {
                    GatewayRoute gatewayRoute = new GatewayRoute();
                    entity.toBean(gatewayRoute, true);
                    routeList.add(gatewayRoute);
                }
            });
            log.info("查询到服务数量为:{}条", entityList.size());
        } catch (SQLException e) {
            log.error("查询路由失败!", e);
        } finally {
            DbUtil.close(conn);
        }
        return routeList;
    }

    @Override
    public List<RateLimitApi> getLimitApiList() {
        List<RateLimitApi> rateLimitApis = Lists.newArrayList();
        //获取默认数据源
        Connection conn = null;
        try {
            conn = ds.getConnection();
            /* 执行查询语句，返回实体列表，一个Entity对象表示一行的数据，Entity对象是一个继承自HashMap的对象，存储的key为字段名，value为字段值 */
            List<Entity> entityList = SqlExecutor.query(conn, SELECT_LIMIT_PATH,
                    new EntityListHandler());
            entityList.forEach(new Consumer<Entity>() {
                @Override
                public void accept(Entity entity) {
                    RateLimitApi rateLimitApi = new RateLimitApi();
                    entity.toBean(rateLimitApi, true);
                    rateLimitApis.add(rateLimitApi);
                }
            });
            log.info("查询到API数量为:{}条", entityList.size());
        } catch (SQLException e) {
            log.error("查询到API成功!", e);
        } finally {
            DbUtil.close(conn);
        }
        return rateLimitApis;
    }


}
