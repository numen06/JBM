package com.jbm.micro.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbm.micro.mysql.mapper.MdFormRowMapper;
import com.jbm.micro.mysql.mapper.MdSampleMapper;
import com.jbm.micro.mysql.mapper.MdTenantDemoMapper;
import com.jbm.micro.mysql.mp.MdFormRow;
import com.jbm.micro.mysql.mp.MdSample;
import com.jbm.micro.mysql.mp.MdTenantDemo;
import com.jbm.micro.mysql.service.MdFormRowService;
import com.jbm.micro.mysql.service.MdSampleService;
import com.jbm.micro.mysql.service.MdTenantDemoService;
import com.jbm.micro.mysql.tenant.DemoTenantLineHandler;
import com.jbm.micro.mysql.web.dto.CreateMdFormRowRequest;
import com.jbm.micro.mysql.web.dto.CreateMdSampleRequest;
import com.jbm.micro.mysql.web.dto.CreateMdTenantDemoRequest;
import com.jbm.micro.mysql.web.dto.MdFormRowResponse;
import com.jbm.micro.mysql.web.dto.MdTenantDemoResponse;
import com.jbm.micro.mysql.web.dto.UpdateMdFormRowRequest;
import com.jbm.micro.mysql.web.filter.DemoTenantIdHeaderFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单数据源 {@code h2}：所有 POJO / DTO、Mapper、Service、Controller 各走一遍实际读写与 JSON 往返。
 */
@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class MicroMysqlDemoFullStackIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MdSampleMapper mdSampleMapper;

    @Autowired
    private MdSampleService mdSampleService;

    @Autowired
    private MdFormRowMapper mdFormRowMapper;

    @Autowired
    private MdFormRowService mdFormRowService;

    @Autowired
    private MdTenantDemoMapper mdTenantDemoMapper;

    @Autowired
    private MdTenantDemoService mdTenantDemoService;

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void clearTenant() {
        DemoTenantLineHandler.clear();
    }

    @Test
    void dtosAndEntities_roundTripJson() throws Exception {
        CreateMdSampleRequest a = new CreateMdSampleRequest();
        a.setName("n");
        a.setFormJson("{}");
        assertEquals("n", objectMapper.readValue(objectMapper.writeValueAsBytes(a), CreateMdSampleRequest.class).getName());

        CreateMdFormRowRequest b = new CreateMdFormRowRequest();
        b.setPayload(Collections.singletonMap("k", 1));
        assertEquals(1, objectMapper.readValue(objectMapper.writeValueAsBytes(b), CreateMdFormRowRequest.class).getPayload().get("k"));

        UpdateMdFormRowRequest c = new UpdateMdFormRowRequest();
        c.setPayload(Collections.singletonMap("x", "y"));
        assertEquals("y", objectMapper.readValue(objectMapper.writeValueAsBytes(c), UpdateMdFormRowRequest.class).getPayload().get("x"));

        MdFormRowResponse d = new MdFormRowResponse();
        d.setId(1L);
        d.setPayload(Collections.singletonMap("p", true));
        assertTrue(objectMapper.readValue(objectMapper.writeValueAsBytes(d), MdFormRowResponse.class).getPayload().containsKey("p"));

        CreateMdTenantDemoRequest e = new CreateMdTenantDemoRequest();
        e.setName("t");
        e.setRemark("r");
        assertEquals("r", objectMapper.readValue(objectMapper.writeValueAsBytes(e), CreateMdTenantDemoRequest.class).getRemark());

        MdTenantDemoResponse f = new MdTenantDemoResponse();
        f.setId(2L);
        f.setTenantId(9L);
        assertEquals(9L, objectMapper.readValue(objectMapper.writeValueAsBytes(f), MdTenantDemoResponse.class).getTenantId());

        MdSample s = new MdSample();
        s.setName("ent");
        s.setFormJson("[]");
        assertEquals("ent", objectMapper.readValue(objectMapper.writeValueAsBytes(s), MdSample.class).getName());

        MdFormRow row = new MdFormRow();
        row.setPayload(Collections.singletonMap("a", 3));
        assertEquals(3, objectMapper.readValue(objectMapper.writeValueAsBytes(row), MdFormRow.class).getPayload().get("a"));

        MdTenantDemo t = new MdTenantDemo();
        t.setName("tn");
        t.setTenantId(1L);
        t.setRemark("rm");
        assertEquals("rm", objectMapper.readValue(objectMapper.writeValueAsBytes(t), MdTenantDemo.class).getRemark());
    }

    @Test
    void mdSample_mapperThenServiceThenHttp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MdSample viaMapper = new MdSample();
        viaMapper.setName("mapper-" + suffix);
        viaMapper.setFormJson("{\"m\":1}");
        mdSampleMapper.insert(viaMapper);
        assertNotNull(viaMapper.getId());

        MdSample viaService = mdSampleService.getById(viaMapper.getId());
        assertNotNull(viaService);
        assertEquals("mapper-" + suffix, viaService.getName());

        CreateMdSampleRequest req = new CreateMdSampleRequest();
        req.setName("http-" + suffix);
        req.setFormJson("{}");
        ResponseEntity<MdSample> post = restTemplate.postForEntity("/api/h2/mp/samples", req, MdSample.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        assertNotNull(post.getBody().getId());
        assertEquals("http-" + suffix, post.getBody().getName());
    }

    @Test
    void mdFormRow_mapperThenServiceThenHttp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MdFormRow viaMapper = new MdFormRow();
        Map<String, Object> p = new HashMap<>();
        p.put("tag", "mapper-" + suffix);
        viaMapper.setPayload(p);
        mdFormRowMapper.insert(viaMapper);
        assertNotNull(viaMapper.getId());

        MdFormRow viaService = mdFormRowService.getById(viaMapper.getId());
        assertNotNull(viaService);
        assertEquals("mapper-" + suffix, viaService.getPayload().get("tag"));

        CreateMdFormRowRequest create = new CreateMdFormRowRequest();
        Map<String, Object> p2 = new HashMap<>();
        p2.put("tag", "http-" + suffix);
        create.setPayload(p2);
        ResponseEntity<MdFormRowResponse> post = restTemplate.postForEntity("/api/h2/mp/form-rows", create, MdFormRowResponse.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        Long id = post.getBody().getId();
        assertEquals("http-" + suffix, post.getBody().getPayload().get("tag"));

        UpdateMdFormRowRequest upd = new UpdateMdFormRowRequest();
        upd.setPayload(Collections.singletonMap("only", "v"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<MdFormRowResponse> put = restTemplate.exchange(
                "/api/h2/mp/form-rows/" + id, HttpMethod.PUT, new HttpEntity<>(upd, headers), MdFormRowResponse.class);
        assertEquals(HttpStatus.OK, put.getStatusCode());
        assertEquals("v", put.getBody().getPayload().get("only"));
    }

    @Test
    void mdTenantDemo_mapperThenServiceThenHttp_withTenantHeader() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long tenant = Math.abs(UUID.randomUUID().getLeastSignificantBits() % 900_000L) + 100_000L;

        DemoTenantLineHandler.setTenantId(tenant);
        MdTenantDemo viaMapper = new MdTenantDemo();
        viaMapper.setName("mapper-tenant-" + suffix);
        viaMapper.setRemark("mr");
        mdTenantDemoMapper.insert(viaMapper);
        assertNotNull(viaMapper.getId());
        MdTenantDemo afterInsert = mdTenantDemoMapper.selectById(viaMapper.getId());
        assertNotNull(afterInsert);
        assertNotNull(afterInsert.getTenantId());

        MdTenantDemo viaService = mdTenantDemoService.getById(viaMapper.getId());
        assertNotNull(viaService);
        assertEquals("mapper-tenant-" + suffix, viaService.getName());
        DemoTenantLineHandler.clear();

        HttpHeaders headers = new HttpHeaders();
        headers.set(DemoTenantIdHeaderFilter.HEADER_NAME, String.valueOf(tenant));
        headers.setContentType(MediaType.APPLICATION_JSON);
        CreateMdTenantDemoRequest body = new CreateMdTenantDemoRequest();
        body.setName("http-tenant-" + suffix);
        body.setRemark("hr");
        ResponseEntity<MdTenantDemoResponse> post = restTemplate.postForEntity(
                "/api/h2/mp/tenant-demos", new HttpEntity<>(body, headers), MdTenantDemoResponse.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        assertNotNull(post.getBody().getId());
        assertEquals("http-tenant-" + suffix, post.getBody().getName());
        assertEquals("hr", post.getBody().getRemark());
        assertEquals(Long.valueOf(tenant), post.getBody().getTenantId());

        ResponseEntity<MdTenantDemoResponse[]> list = restTemplate.exchange(
                "/api/h2/mp/tenant-demos", HttpMethod.GET, new HttpEntity<>(headers), MdTenantDemoResponse[].class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertNotNull(list.getBody());
        assertTrue(list.getBody().length >= 1);
    }
}
