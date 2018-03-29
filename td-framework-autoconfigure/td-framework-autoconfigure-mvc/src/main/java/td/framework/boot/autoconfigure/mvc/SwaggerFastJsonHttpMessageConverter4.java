package td.framework.boot.autoconfigure.mvc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;
import com.alibaba.fastjson.support.springfox.SwaggerJsonSerializer;

import springfox.documentation.spring.web.json.Json;

public class SwaggerFastJsonHttpMessageConverter4 extends FastJsonHttpMessageConverter4 {

	public SwaggerFastJsonHttpMessageConverter4() {
		super();
		List<MediaType> aa = new ArrayList<>();
		aa.add(MediaType.APPLICATION_JSON);
		aa.add(MediaType.APPLICATION_JSON_UTF8);
		aa.add(MediaType.TEXT_HTML);
		this.setSupportedMediaTypes(aa);
		this.getFastJsonConfig().setSerializerFeatures(SerializerFeature.BrowserCompatible, SerializerFeature.PrettyFormat, SerializerFeature.DisableCircularReferenceDetect);
		try {
			ClassUtils.forName("springfox.documentation.spring.web.json.Json", ClassUtils.getDefaultClassLoader());
			this.getFastJsonConfig().getSerializeConfig().put(Json.class, SwaggerJsonSerializer.instance);
			// fastJsonConfig.getSerializeConfig().put(springfox.documentation.swagger.web.UiConfiguration.class,
			// new SwaggerConfigurationSerializer(fastConverter));
		} catch (ClassNotFoundException | LinkageError e) {
			logger.warn("swagger is not found");
		}
	}

}
