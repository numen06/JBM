package com.jajachina.test.td;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import jbm.framework.boot.autoconfigure.td.StableExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TdTest {

    private Connection conn;

    @BeforeEach
    public void before() throws SQLException {
        conn = TDengineConnection.getConnection();
    }

    @Test
    public void test() {
        try (Connection conn = TDengineConnection.getConnection()) {
            String insertSQL = "insert into d1001 using devices tags ('测试','1','1') (ts, status)  values ( \"2018-10-03 14:38:05\", 1)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.execute(insertSQL);
                System.out.println("Batch data inserted successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInsert() throws SQLException {
        StableExecutor executor = new StableExecutor(conn, "devices");
        Map<String, Object> map = MapUtil.newHashMap();
        map.put("device_id", "1");
        map.put("device_type", "type1");
        map.put("device_code", "code1");
        map.put("status", RandomUtil.randomInt(1, 10));
        map.put("ts", System.currentTimeMillis());
        executor.insertSubTable((d) -> "device_type_" + d.get("device_type").toString(), map);
    }

    @Test
    public void testInsertBatch() throws SQLException {

        StableExecutor executor = new StableExecutor(conn, "devices");

        List<Map<String, Object>> list = CollUtil.newArrayList();

        Map<String, Object> map = MapUtil.newHashMap();
        map.put("device_id", "1");
        map.put("device_type", "type1");
        map.put("device_code", "code1");
        map.put("status", RandomUtil.randomInt(1, 10));
        map.put("ts", System.currentTimeMillis());
        map.put("cpu", RandomUtil.randomDouble(1, 100));

        list.add(map);
        Map<String, Object> map2 = MapUtil.newHashMap();
        map2.put("device_id", "2");
        map2.put("device_type", "type2");
        map2.put("device_code", "code2");
        map2.put("status", RandomUtil.randomInt(1, 10));
        map2.put("ts", System.currentTimeMillis());
        map2.put("temperature", RandomUtil.randomDouble(10, 30));

        list.add(map2);

        executor.insertSubTableBatch((d) -> "device_type_" + d.get("device_type").toString(), list);


    }

}
