package com.jajachina.test.td;

import cn.hutool.db.ds.simple.SimpleDataSource;
import com.taosdata.jdbc.rs.RestfulDriver;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @author wesley
 */
public class TDengineConnection {
    private static final String URL = "jdbc:TAOS-RS://10.100.10.62:6041/iot";
    private static final String USER = "root";
    private static final String PASSWORD = "taosdata";

    public static DataSource getConnection() throws SQLException {
        return new SimpleDataSource(URL, USER, PASSWORD, RestfulDriver.class.getName());
    }
}
