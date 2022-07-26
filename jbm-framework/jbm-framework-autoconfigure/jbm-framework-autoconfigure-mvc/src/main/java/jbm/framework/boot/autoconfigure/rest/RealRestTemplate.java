package jbm.framework.boot.autoconfigure.rest;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 真正的RestTemplate实现
 *
 * @author wesley
 */
public class RealRestTemplate extends RestTemplate {

    public RealRestTemplate() {
        super();
        RestTemplateFactory.initMessageConverters(this);
//		RestTemplateFactory.initHttps(this);
    }

    public RealRestTemplate(ClientHttpRequestFactory requestFactory) {
        super(requestFactory);
        RestTemplateFactory.initMessageConverters(this);
//		RestTemplateFactory.initHttps(this);
    }

    public RealRestTemplate(List<HttpMessageConverter<?>> messageConverters) {
        super(messageConverters);
        RestTemplateFactory.initMessageConverters(this);
//		RestTemplateFactory.initHttps(this);
    }

    public <T> T postJsonForObject(String url, Object request, Class<T> responseType, Object... uriVariables) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<Object> httpEntity = new HttpEntity<Object>(request, headers);
        return super.postForEntity(url, httpEntity, responseType, uriVariables).getBody();
    }

    @Override
    public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) throws RestClientException {
        if (request instanceof HttpEntity) {
            return super.postForEntity(url, request, responseType, uriVariables).getBody();
        }
        return this.postJsonForObject(url, request, responseType, uriVariables);
    }

    public <T> T postMultipleForObject(String url, MultiValueMap<String, Object> request, Class<T> responseType) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<Object> httpEntity = new HttpEntity<Object>(request, headers);
        return this.postForObject(url, httpEntity, responseType);
    }

}
