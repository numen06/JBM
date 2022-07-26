package jbm.framework.boot.autoconfigure.rest;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import jbm.framework.boot.autoconfigure.fastjson.FastJsonConfiguration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.function.Predicate;

public class RestTemplateFactory {

    private static final FastJsonHttpMessageConverter fastJsonHttpMessageConverter = FastJsonConfiguration.getFastJsonHttpMessageConverter();

//	private static TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
//		@Override
//		public boolean isTrusted(X509Certificate[] chain, String loginType) throws CertificateException {
//			return true;
//		}
//	};
    private static final FileFormHttpMessageConverter fileFormHttpMessageConverter = new FileFormHttpMessageConverter();
    private static RestTemplateFactory restTemplateFactory;

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

//	public static void initHttps(RestTemplate restTemplate) {
//		try {
//			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
//			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
//			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
//			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
//			requestFactory.setHttpClient(httpClient);
//			restTemplate.setRequestFactory(requestFactory);
//		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
//		}
//	}

    public RealRestTemplate createRealRestTemplate() {
        RealRestTemplate restTemplate = new RealRestTemplate();
        return restTemplate;
    }

    public RestTemplate createRestTemplate() {
        return createRealRestTemplate();
    }
}
