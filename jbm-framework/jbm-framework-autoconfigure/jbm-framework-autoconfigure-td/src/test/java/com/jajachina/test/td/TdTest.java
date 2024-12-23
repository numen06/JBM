package com.jajachina.test.td;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import jbm.framework.boot.autoconfigure.td.StableExecutor;
import jbm.framework.boot.autoconfigure.td.TdTemplate;
import jbm.framework.boot.autoconfigure.td.configuration.TDProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TdTest {

    private TdTemplate tdTemplate;

    @BeforeEach
    public void before() throws Exception {

        TDProperties tdProperties = new TDProperties();
        tdProperties.setUrl("jdbc:TAOS-RS://10.100.10.62:6041/iot");
        tdProperties.setUsername("root");
        tdProperties.setPassword("taosdata");
        tdTemplate = new TdTemplate(tdProperties);
        tdTemplate.afterPropertiesSet();
    }

    @Test
    public void test() {
        try (Connection conn = tdTemplate.getDataSource().getConnection()) {
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
        StableExecutor executor = tdTemplate.getSTableExecutor("devices");
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

        StableExecutor executor = tdTemplate.getSTableExecutor("devices");

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

    @Test
    public void testMoreInsert() throws SQLException {

//        (pt, voltage_unbalance, uc_harmonic_distortion_rate, power_factor, power_a, power_b, voltage_a, power_c,
//        voltage_b, current_a, ib_harmonic_distortion_rate, voltage_c, ic_harmonic_distortion_rate,
//        current_b, ub_harmonic_distortion_rate, current_c, current_unbalance, cumulative_energy,
//        ia_harmonic_distortion_rate, ct, ua_harmonic_distortion_rate, power_no, power, ts)
        StableExecutor executor = tdTemplate.getSTableExecutor("devices");

        Map<String, Object> map = MapUtil.newHashMap();
        map.put("device_id", "2");
        map.put("device_type", "1816362047206592513");
        map.put("device_code", "code2");
        map.put("pt", RandomUtil.randomInt(1, 10));
        map.put("voltage_unbalance", RandomUtil.randomDouble(1, 10));
        map.put("uc_harmonic_distortion_rate", RandomUtil.randomDouble(1, 10));
        map.put("power_factor", RandomUtil.randomDouble(1, 10));
        map.put("power_a", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1), BigDecimal.valueOf(10)));
        map.put("power_b", RandomUtil.randomDouble(1, 10));
        map.put("power_c", RandomUtil.randomDouble(1, 10));
        map.put("voltage_a", RandomUtil.randomDouble(1, 10));
        map.put("voltage_b", RandomUtil.randomDouble(1, 10));
        map.put("voltage_c", RandomUtil.randomDouble(1, 10));
        map.put("current_a", RandomUtil.randomDouble(1, 10));
        map.put("current_b", RandomUtil.randomDouble(1, 10));
        map.put("current_c", RandomUtil.randomDouble(1, 10));
        map.put("ib_harmonic_distortion_rate", RandomUtil.randomDouble(1, 10));
        map.put("ic_harmonic_distortion_rate", RandomUtil.randomDouble(1, 10));
        map.put("ia_harmonic_distortion_rate", RandomUtil.randomDouble(1, 10));
        map.put("ua_harmonic_distortion_rate", RandomUtil.randomDouble(1, 10));
        map.put("current_unbalance", RandomUtil.randomDouble(1, 10));
        map.put("cumulative_energy", RandomUtil.randomDouble(1, 10));
        map.put("ct", RandomUtil.randomDouble(1, 10));
        map.put("power_no", RandomUtil.randomDouble(1, 10));
        map.put("power", RandomUtil.randomDouble(1, 10));
        map.put("voltage", RandomUtil.randomDouble(1, 10));
        map.put("ts", System.currentTimeMillis());

        executor.insertSubTable((d) -> "device_type_" + d.get("device_type").toString(), map);

    }


}
