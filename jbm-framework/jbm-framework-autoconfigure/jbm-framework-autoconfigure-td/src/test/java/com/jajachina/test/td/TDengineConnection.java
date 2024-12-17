package com.jajachina.test.td;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author wesley
 */
public class TDengineConnection {
    private static final String URL = "jdbc:TAOS-RS://10.100.10.62:6041/iot";
    private static final String USER = "root";
    private static final String PASSWORD = "taosdata";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
