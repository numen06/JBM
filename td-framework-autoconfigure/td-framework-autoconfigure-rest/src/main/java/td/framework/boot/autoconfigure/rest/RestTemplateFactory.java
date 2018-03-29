package td.framework.boot.autoconfigure.rest;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Predicate;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

public class RestTemplateFactory {

	private static RestTemplateFactory restTemplateFactory;

	private static TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			return true;
		}
	};

	private static final FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
	private static final FileFormHttpMessageConverter fileFormHttpMessageConverter = new FileFormHttpMessageConverter();

	private RestTemplateFactory() {
		fileFormHttpMessageConverter.addPartConverter(new FileHttpMessageConverter());
	}

	public static RestTemplateFactory getInstance() {
		if (restTemplateFactory == null)
			restTemplateFactory = new RestTemplateFactory();
		return restTemplateFactory;
	}

	public static void initMessageConverters(RestTemplate restTemplate) {
		restTemplate.getMessageConverters().removeIf(new Predicate<HttpMessageConverter<?>>() {
			@Override
			public boolean test(HttpMessageConverter<?> t) {
				if (t instanceof StringHttpMessageConverter)
					return true;
				if (t instanceof FormHttpMessageConverter) {
					return true;
				}
				return false;
			}
		});
		restTemplate.getMessageConverters().add(1, new StringHttpMessageConverter(Charset.defaultCharset()));
		restTemplate.getMessageConverters().add(fileFormHttpMessageConverter);
		restTemplate.getMessageConverters().add(fastJsonHttpMessageConverter);
	}

	public static void initHttps(RestTemplate restTemplate) {
		try {
			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			restTemplate.setRequestFactory(requestFactory);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
		}
	}

	public RealRestTemplate createRealRestTemplate() {
		RealRestTemplate restTemplate = new RealRestTemplate();
		return restTemplate;
	}

	public RestTemplate createRestTemplate() {
		return createRealRestTemplate();
	}
}
