package jbm.framework.boot.autoconfigure.td;

import cn.hutool.db.ds.simple.SimpleDataSource;
import com.taosdata.jdbc.rs.RestfulDriver;
import jbm.framework.boot.autoconfigure.td.configuration.TDProperties;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.SQLException;

public class TdTemplate implements InitializingBean {

    private TDProperties tdProperties;

    private DataSource dataSource;

    public TdTemplate(TDProperties tdProperties) {
        this.tdProperties = tdProperties;
    }

    public StableExecutor getSTableExecutor(String stableName) throws SQLException {
        return new StableExecutor(this.dataSource, stableName);
    }

    public DataSource createDataSource(TDProperties properties) throws SQLException {
        return new SimpleDataSource(properties.getUrl(), properties.getUsername(), properties.getPassword(), RestfulDriver.class.getName());
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.dataSource = createDataSource(tdProperties);
    }
}
