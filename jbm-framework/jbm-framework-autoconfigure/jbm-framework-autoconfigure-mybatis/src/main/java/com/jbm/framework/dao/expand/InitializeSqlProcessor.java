package com.jbm.framework.dao.expand;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.framework.dao.JdbcDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.io.IOException;

@Slf4j
public class InitializeSqlProcessor implements BeanPostProcessor {

    private DataSource ds;


    public InitializeSqlProcessor() {

    }

    public void initialize() {
        try {
            SqlPrepareRunner sqlPrepareRunner = new SqlPrepareRunner(ds);
            sqlPrepareRunner.scanSqlFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (ds == null) {
            JdbcDataSourceProperties dataSource = SpringUtil.getBean(JdbcDataSourceProperties.class);
            this.ds = new SimpleDataSource(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
            this.initialize();
        }
        return bean;
    }

}
