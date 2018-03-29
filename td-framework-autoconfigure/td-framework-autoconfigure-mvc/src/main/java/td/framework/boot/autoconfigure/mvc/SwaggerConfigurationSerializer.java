package td.framework.boot.autoconfigure.mvc;

import java.io.IOException;
import java.lang.reflect.Type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author wesley.zhang
 * @date 2017年10月18日
 * @version 1.0
 *
 */
public class SwaggerConfigurationSerializer implements ObjectSerializer {
	public final static SwaggerConfigurationSerializer INSTANCE = new SwaggerConfigurationSerializer();

	@Override
	public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
		SerializeWriter out = serializer.getWriter();
		// String value = Jackson.getObjectMapper().writeValueAsString(object);
		String value = JSON.toJSONString(object, SerializerFeature.DisableCircularReferenceDetect);
		out.write(value);
	}

}
