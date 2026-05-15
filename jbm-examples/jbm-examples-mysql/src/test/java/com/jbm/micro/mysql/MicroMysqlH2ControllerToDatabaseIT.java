package com.jbm.micro.mysql;

import com.jbm.micro.mysql.mp.MdSample;
import com.jbm.micro.mysql.web.dto.CreateMdFormRowRequest;
import com.jbm.micro.mysql.web.dto.CreateMdSampleRequest;
import com.jbm.micro.mysql.web.dto.MdFormRowResponse;
import com.jbm.micro.mysql.web.dto.UpdateMdFormRowRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2 + {@code h2} profile：从 HTTP Controller 经 Service 到数据库（仅 MyBatis-Plus + Liquibase）。
 */
@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class MicroMysqlH2ControllerToDatabaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void mpSampleControllerCreatesAndListsMdSample() {
        CreateMdSampleRequest req = new CreateMdSampleRequest();
        req.setName("it-mp-sample");
        req.setFormJson("{\"from\":\"it\"}");

        ResponseEntity<MdSample> post = restTemplate.postForEntity(
                "/api/h2/mp/samples", req, MdSample.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        assertNotNull(post.getBody().getId());
        assertEquals("it-mp-sample", post.getBody().getName());

        ResponseEntity<MdSample[]> list = restTemplate.getForEntity(
                "/api/h2/mp/samples", MdSample[].class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertNotNull(list.getBody());
        assertTrue(list.getBody().length >= 1);
    }

    @Test
    void mdFormRowJsonFieldFullCrudUsesDtos() {
        CreateMdFormRowRequest create = new CreateMdFormRowRequest();
        Map<String, Object> payload = new HashMap<>();
        payload.put("demo", "crud-1");
        payload.put("n", 2);
        create.setPayload(payload);

        ResponseEntity<MdFormRowResponse> post = restTemplate.postForEntity(
                "/api/h2/mp/form-rows", create, MdFormRowResponse.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        Long id = post.getBody().getId();
        assertEquals("crud-1", post.getBody().getPayload().get("demo"));

        ResponseEntity<MdFormRowResponse> get = restTemplate.getForEntity(
                "/api/h2/mp/form-rows/" + id, MdFormRowResponse.class);
        assertEquals(HttpStatus.OK, get.getStatusCode());
        assertNotNull(get.getBody());
        assertEquals(2, ((Number) get.getBody().getPayload().get("n")).intValue());

        UpdateMdFormRowRequest update = new UpdateMdFormRowRequest();
        Map<String, Object> payload2 = new HashMap<>();
        payload2.put("demo", "updated");
        update.setPayload(payload2);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateMdFormRowRequest> putEntity = new HttpEntity<>(update, headers);
        ResponseEntity<MdFormRowResponse> put = restTemplate.exchange(
                "/api/h2/mp/form-rows/" + id, HttpMethod.PUT, putEntity, MdFormRowResponse.class);
        assertEquals(HttpStatus.OK, put.getStatusCode());
        assertNotNull(put.getBody());
        assertEquals("updated", put.getBody().getPayload().get("demo"));

        restTemplate.delete("/api/h2/mp/form-rows/" + id);

        ResponseEntity<MdFormRowResponse> afterDelete = restTemplate.getForEntity(
                "/api/h2/mp/form-rows/" + id, MdFormRowResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
    }

    @Test
    void mdFormRowCreateAllowsEmptyPayloadInDto() {
        CreateMdFormRowRequest req = new CreateMdFormRowRequest();
        req.setPayload(Collections.emptyMap());
        ResponseEntity<MdFormRowResponse> post = restTemplate.postForEntity(
                "/api/h2/mp/form-rows", req, MdFormRowResponse.class);
        assertEquals(HttpStatus.OK, post.getStatusCode());
        assertNotNull(post.getBody());
        assertNotNull(post.getBody().getId());
    }

    @Test
    void mdFormRowListReturnsDtoArray() {
        CreateMdFormRowRequest req = new CreateMdFormRowRequest();
        Map<String, Object> p = new HashMap<>();
        p.put("k", "list-test");
        req.setPayload(p);
        restTemplate.postForEntity("/api/h2/mp/form-rows", req, MdFormRowResponse.class);

        ResponseEntity<MdFormRowResponse[]> list = restTemplate.getForEntity(
                "/api/h2/mp/form-rows", MdFormRowResponse[].class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertNotNull(list.getBody());
        assertTrue(list.getBody().length >= 1);
    }
}
