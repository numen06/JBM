package com.jbm.framework.dao.expand;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.framework.dao.JdbcDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

/**
 * @author wesley
 */
@Slf4j
public class InitializeSqlProcessor implements BeanPostProcessor {

    private DataSource ds;

    public InitializeSqlProcessor() {

    }

    public void initialize() {
        try {
            SqlPrepareRunner sqlPrepareRunner = new SqlPrepareRunner(ds);
            sqlPrepareRunner.scanSqlFiles();
        } catch (Exception e) {
            log.error("初始化数据库文件扫描失败");
//            throw new RuntimeException(e);
        }
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (ds == null) {
            JdbcDataSourceProperties dataSource = SpringUtil.getBean(JdbcDataSourceProperties.class);
            if (dataSource.getUrl() == null) {
//                log.warn("数据源未配置，不执行初始化数据库文件扫描");
                return bean;
            }
            this.ds = new SimpleDataSource(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
            this.initialize();
        }
        return bean;
    }

}
