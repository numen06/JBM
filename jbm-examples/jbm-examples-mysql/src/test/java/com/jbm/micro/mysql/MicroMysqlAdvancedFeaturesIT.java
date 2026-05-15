package com.jbm.micro.mysql;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.micro.mysql.mapper.MdTenantDemoMapper;
import com.jbm.micro.mysql.mapper.SlaveProbeMapper;
import com.jbm.micro.mysql.mp.MdTenantDemo;
import com.jbm.micro.mysql.tenant.DemoTenantLineHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 多数据源（主从 H2）、多租户（{@link DemoTenantLineHandler}）、Liquibase 字段追加（{@code remark} / V4）联调。
 * <p>使用独立 profile，避免与默认单库 {@code h2} 配置冲突。</p>
 */
@SpringBootTest(
        classes = MicroMysqlApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=h2-advanced"
)
class MicroMysqlAdvancedFeaturesIT {

    @Autowired
    private SlaveProbeMapper slaveProbeMapper;

    @Autowired
    private MdTenantDemoMapper mdTenantDemoMapper;

    @AfterEach
    void tearDown() {
        DemoTenantLineHandler.clear();
    }

    @Test
    void slaveDataSourceRoutesToSecondH2() {
        assertEquals(Integer.valueOf(1), slaveProbeMapper.ping());
    }

    @Test
    void tenantLineFiltersSelectAcrossTenants() {
        DemoTenantLineHandler.setTenantId(100L);
        MdTenantDemo row = new MdTenantDemo();
        row.setName("tenant-row-a");
        mdTenantDemoMapper.insert(row);
        assertNotNull(row.getId());

        assertEquals(1, mdTenantDemoMapper.selectCount(new QueryWrapper<MdTenantDemo>().eq("name", "tenant-row-a")));

        DemoTenantLineHandler.setTenantId(200L);
        assertEquals(0, mdTenantDemoMapper.selectCount(new QueryWrapper<MdTenantDemo>().eq("name", "tenant-row-a")));

        DemoTenantLineHandler.setTenantId(100L);
        assertEquals(1, mdTenantDemoMapper.selectCount(new QueryWrapper<MdTenantDemo>().eq("name", "tenant-row-a")));
    }

    @Test
    void liquibaseV4RemarkColumnWritable() {
        DemoTenantLineHandler.setTenantId(300L);
        MdTenantDemo row = new MdTenantDemo();
        row.setName("with-remark");
        row.setRemark("from-test");
        mdTenantDemoMapper.insert(row);

        MdTenantDemo loaded = mdTenantDemoMapper.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("from-test", loaded.getRemark());
    }
}
