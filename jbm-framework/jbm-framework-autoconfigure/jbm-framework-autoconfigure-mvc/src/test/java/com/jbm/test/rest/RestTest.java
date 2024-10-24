package com.jbm.test.rest;

import jbm.framework.boot.autoconfigure.rest.RealRestTemplate;
import jbm.framework.boot.autoconfigure.rest.RestTemplateFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class RestTest {
    RealRestTemplate restTemplate = RestTemplateFactory.getInstance().createRealRestTemplate();

    @Test
    public void testJSON() throws RestClientException, UnsupportedEncodingException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("deviceTypeId", 1);
        String url = "http://www.51jbm.com/jbm-center-web/data/org/getManufacturerOrgsByDeviceTypeId";
        String returnJsonStr = restTemplate.postJsonForObject(url, map, String.class);
        System.err.println(returnJsonStr);
    }

    @Test
    public void testHTML() throws RestClientException, UnsupportedEncodingException {
        String url = "http://www.51jbm.com/jbm-center-web/";
        String returnJsonStr = restTemplate.getForObject(url, String.class);
        System.err.println(returnJsonStr);
    }

    @Test
    public void testHttps() throws RestClientException, UnsupportedEncodingException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("deviceTypeId", 1);
        String url = "https://www.51jbm.com/jbm-center-web/data/org/getManufacturerOrgsByDeviceTypeId";
        String returnJsonStr = restTemplate.postJsonForObject(url, map, String.class);
        System.err.println(returnJsonStr);
    }

    @Test
    public void testUploadFile() throws RestClientException, UnsupportedEncodingException {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("file[0]", new File("美图图库/示例图片_01.jpg"));
        map.add("file[1]", new FileSystemResource(new File("美图图库/示例图片_02.jpg")));
        map.add("file[2]", new FileSystemResource(new File("美图图库/示例图片_03.jpg")));
//		String url = "https://doc.51jbm.com/api/upload";
        String url = "http://127.0.0.1:20000/api/upload";
        String returnJsonStr = restTemplate.postMultipleForObject(url, map, String.class);
        System.err.println(returnJsonStr);
    }
}
