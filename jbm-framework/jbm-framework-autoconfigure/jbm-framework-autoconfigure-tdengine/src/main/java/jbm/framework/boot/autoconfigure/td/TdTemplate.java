package jbm.framework.boot.autoconfigure.td;

import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.hikari.HikariDSFactory;
import cn.hutool.setting.Setting;
import com.taosdata.jdbc.rs.RestfulDriver;
import jbm.framework.boot.autoconfigure.td.configuration.TDProperties;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * ## 基本配置信息
 * # JDBC URL，根据不同的数据库，使用相应的JDBC连接字符串
 * url = jdbc:mysql://<host>:<port>/<database_name>
 * # 用户名，此处也可以使用 user 代替
 * username = 用户名
 * # 密码，此处也可以使用 pass 代替
 * password = 密码
 * # JDBC驱动名，可选（Hutool会自动识别）
 * driver = com.mysql.jdbc.Driver
 *
 * @author wesley
 */
public class TdTemplate implements InitializingBean {

    private TDProperties tdProperties;

    private DSFactory dsFactory;

    private DataSource dataSource;

    public TdTemplate(TDProperties tdProperties) {
        this.tdProperties = tdProperties;
    }

    public TdTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public StableExecutor getSTableExecutor(String stableName) throws SQLException {
        return new StableExecutor(this.getDataSource(), stableName);
    }

    public DataSource getDataSource() throws SQLException {
        if (dataSource != null) {
            return dataSource;
        }
        return dsFactory.getDataSource();
//        return new SimpleDataSource(properties.getUrl(), properties.getUsername(), properties.getPassword(), RestfulDriver.class.getName());
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (tdProperties == null) {
            return;
        }
        Setting setting = new Setting();
        setting.put("url", tdProperties.getUrl());
        setting.put("driver", RestfulDriver.class.getName());
        setting.put("username", tdProperties.getUsername());
        setting.put("password", tdProperties.getPassword());
        dsFactory = DSFactory.setCurrentDSFactory(new HikariDSFactory(setting));
    }
}
