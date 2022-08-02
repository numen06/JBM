package jbm.framework.boot.autoconfigure.fastjson;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.springfox.SwaggerJsonSerializer;
import com.jbm.util.bean.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: jbm
 * @author: wesley.zhang
 * @create: 2020-02-16 02:40
 **/
@Slf4j
@ConditionalOnClass(FastJsonHttpMessageConverter.class)
public class FastJsonConfiguration {

    private static FastJsonHttpMessageConverter fastConverter;

    public static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        return getFastJsonHttpMessageConverter(false);
    }

    public static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter(boolean clean) {

        if (ObjectUtil.isNotEmpty(fastConverter)) {
            if (clean) {
                fastConverter.getFastJsonConfig().getSerializeConfig().clearSerializers();
            } else {
                return fastConverter;
            }
        }
        //创建fastJson消息转换器
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
//        supportedMediaTypes.add(MediaType.APPLICATION_ATOM_XML);
//        supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
//        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
//        supportedMediaTypes.add(MediaType.APPLICATION_PDF);
//        supportedMediaTypes.add(MediaType.APPLICATION_RSS_XML);
//        supportedMediaTypes.add(MediaType.APPLICATION_XHTML_XML);
//        supportedMediaTypes.add(MediaType.APPLICATION_XML);
//        supportedMediaTypes.add(MediaType.IMAGE_GIF);
//        supportedMediaTypes.add(MediaType.IMAGE_JPEG);
//        supportedMediaTypes.add(MediaType.IMAGE_PNG);
//        supportedMediaTypes.add(MediaType.TEXT_EVENT_STREAM);
//        supportedMediaTypes.add(MediaType.TEXT_HTML);
//        supportedMediaTypes.add(MediaType.TEXT_MARKDOWN);
//        supportedMediaTypes.add(MediaType.TEXT_PLAIN);
//        supportedMediaTypes.add(MediaType.TEXT_XML);
//        supportedMediaTypes.add(MediaType.ALL);
        fastConverter.setSupportedMediaTypes(supportedMediaTypes);
//        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
//        parserConfig.putDeserializer(Enum.class, new EnumValueDeserializer(Enum.class));
//
//        SerializeConfig globalInstance = SerializeConfig.getGlobalInstance();
//        globalInstance.put(Enum.class, new EnumValueSerializer());

        //创建配置类
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        //设置枚举
//        fastJsonConfig.setParserConfig(new EnumParserConfig());
//        fastJsonConfig.setSerializeConfig(new EnumSerializeConfig());
        //设置swagger ui
        SerializeConfig serializeConfig = new FastJsonSerializeConfig();
        try {
            Class jsonClass = ClassUtil.loadClass("springfox.documentation.spring.web.json.Json");
            serializeConfig.put(jsonClass, SwaggerJsonSerializer.instance);
        } catch (Exception e) {
            log.warn("没有找到swagger框架");
        }
        serializeConfig.put(BigInteger.class, ToStringSerializer.instance);
        serializeConfig.put(Long.class, ToStringSerializer.instance);
        serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
        serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
        serializeConfig.put(Version.class, ToStringSerializer.instance);
        //修改配置返回内容的过滤
        //WriteNullListAsEmpty  ：List字段如果为null,输出为[],而非null
        //WriteNullStringAsEmpty ： 字符类型字段如果为null,输出为"",而非null
        //DisableCircularReferenceDetect ：消除对同一对象循环引用的问题，默认为false（如果不配置有可能会进入死循环）
        //WriteNullBooleanAsFalse：Boolean字段如果为null,输出为false,而非null
        //WriteMapNullValue：是否输出值为null的字段,默认为false
        fastJsonConfig.setSerializerFeatures(
                SerializerFeature.DisableCircularReferenceDetect
//                SerializerFeature.WriteMapNullValue,
//                SerializerFeature.WriteNullStringAsEmpty,
//                SerializerFeature.WriteNonStringValueAsString
        );
        fastJsonConfig.setSerializeConfig(serializeConfig);
        fastConverter.setDefaultCharset(StandardCharsets.UTF_8);
        fastConverter.setFastJsonConfig(fastJsonConfig);
        FastJsonConfiguration.fastConverter = fastConverter;
        return fastConverter;
    }

    @Bean
    @Primary
    public FastJsonHttpMessageConverter getFastJsonConverter() {
        return FastJsonConfiguration.getFastJsonHttpMessageConverter(true);
    }

//    public static void main(String[] args) throws IOException {
//        DataPaging<Long> date = new DataPaging<>();
//        date.setContents(Lists.newArrayList(10l));
//        OutputStream outputStream = new ByteArrayOutputStream();
//        HttpOutputMessage httpOutputMessage = new HttpOutputMessage() {
//            @Override
//            public OutputStream getBody() throws IOException {
//                return outputStream;
//            }
//
//            @Override
//            public HttpHeaders getHeaders() {
//                return new HttpHeaders();
//            }
//        };
//        FastJsonConfiguration.getFastJsonHttpMessageConverter().write(date, MediaType.APPLICATION_JSON_UTF8, httpOutputMessage);
//        System.out.println(StrUtil.toString(httpOutputMessage.getBody()));
//    }

}
