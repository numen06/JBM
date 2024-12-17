package jbm.framework.boot.autoconfigure.td;

import jbm.framework.boot.autoconfigure.td.configuration.TDProperties;
import org.springframework.beans.factory.InitializingBean;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TdTemplate implements InitializingBean {

    private TDProperties tdProperties;

    private Connection connection;

    public TdTemplate(TDProperties tdProperties) {
        this.tdProperties = tdProperties;
    }

    public StableExecutor getSTableExecutor(String stableName) throws SQLException {
        return new StableExecutor(this.connection, stableName);
    }

    public Connection createConnection(TDProperties properties) throws SQLException {
        return DriverManager.getConnection(tdProperties.getUrl(), tdProperties.getUsername(), tdProperties.getPassword());
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.connection = createConnection(tdProperties);
    }
}
