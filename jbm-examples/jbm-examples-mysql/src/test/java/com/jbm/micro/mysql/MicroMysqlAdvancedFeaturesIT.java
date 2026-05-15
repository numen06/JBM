package com.jbm.micro.mysql;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.micro.mysql.mapper.MdTenantDemoMapper;
import com.jbm.micro.mysql.mapper.SlaveProbeMapper;
import com.jbm.micro.mysql.mp.MdTenantDemo;
import com.jbm.micro.mysql.service.MdTenantDemoService;
import com.jbm.micro.mysql.tenant.DemoTenantLineHandler;
import com.jbm.micro.mysql.web.dto.CreateMdTenantDemoRequest;
import com.jbm.micro.mysql.web.dto.MdTenantDemoResponse;
import com.jbm.micro.mysql.web.filter.DemoTenantIdHeaderFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

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

    @Autowired
    private MdTenantDemoService mdTenantDemoService;

    @Autowired
    private TestRestTemplate restTemplate;

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

    @Test
    void mdTenantDemo_serviceSaveAndGet() {
        DemoTenantLineHandler.setTenantId(400L);
        String name = "svc-" + UUID.randomUUID();
        MdTenantDemo row = new MdTenantDemo();
        row.setName(name);
        row.setRemark("svc-remark");
        mdTenantDemoService.save(row);
        assertNotNull(row.getId());

        MdTenantDemo loaded = mdTenantDemoService.getById(row.getId());
        assertNotNull(loaded);
        assertEquals(name, loaded.getName());
        assertEquals("svc-remark", loaded.getRemark());
    }

    @Test
    void mdTenantDemo_httpUsesSlaveDsAppContext_andTenantHeader() {
        assertEquals(Integer.valueOf(1), slaveProbeMapper.ping());

        HttpHeaders headers = new HttpHeaders();
        headers.set(DemoTenantIdHeaderFilter.HEADER_NAME, "500");
        headers.setContentType(MediaType.APPLICATION_JSON);
        CreateMdTenantDemoRequest body = new CreateMdTenantDemoRequest();
        body.setName("adv-rest-" + UUID.randomUUID());
        body.setRemark("adv");
        ResponseEntity<MdTenantDemoResponse> post = restTemplate.postForEntity(
                "/api/h2/mp/tenant-demos", new HttpEntity<>(body, headers), MdTenantDemoResponse.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        assertNotNull(post.getBody().getId());
        assertEquals(Long.valueOf(500L), post.getBody().getTenantId());
    }
}
