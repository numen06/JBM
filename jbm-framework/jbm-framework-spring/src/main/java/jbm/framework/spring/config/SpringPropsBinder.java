package jbm.framework.spring.config;

import cn.hutool.core.bean.BeanUtil;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class SpringPropsBinder {


    public static void fill(String propsContent, Object obj) {
        Object result = bind(propsContent, obj.getClass());
        BeanUtil.copyProperties(result, obj);
    }

    public static void fill(String propsContent, Object obj, String prefix) {
        Object result = bind(propsContent, obj.getClass(), prefix);
        BeanUtil.copyProperties(result, obj);
    }

    /**
     * 将 properties 字符串（如 "app.base-url=xxx\napp.timeout=10s"）绑定到目标对象
     *
     * @param propsContent properties 内容（UTF-8）
     * @param targetType   目标类（需有 @ConfigurationProperties 或普通 POJO）
     * @param prefix       前缀，如 "app" → 只绑定 app.xxx 的项；为空则绑定全部
     * @param <T>          类型
     * @return 绑定后的新实例（线程安全）
     */
    public static <T> T bind(String propsContent, Class<T> targetType, String prefix) {
        try {
            // 1️⃣ 解析 String → Properties → Map
            Properties props = new Properties();
            props.load(new ByteArrayInputStream(propsContent.getBytes(StandardCharsets.UTF_8)));

            Map<String, Object> rawMap = props.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> (String) e.getValue() // Binder 要求 value 是 String（自动转类型）
                    ));

            // 2️⃣ 构建 Spring 的 ConfigurationPropertySource（支持前缀、层级、占位符）
            DefaultPropertiesPropertySource source = new DefaultPropertiesPropertySource(rawMap);

            // 3️⃣ 创建干净的 Environment（用于占位符解析，如 ${PORT:8080}）
            Environment env = new StandardEnvironment(); // 无 profile，但支持占位符默认值

            // 4️⃣ 绑定（核心：Binder + ConfigurationPropertySources）
            Binder binder = new Binder(ConfigurationPropertySources.from(source));
            BindResult<T> result = binder.bind(prefix, Bindable.of(targetType));

            return result.orElseThrow(() ->
                    new IllegalArgumentException("Failed to bind properties to " + targetType.getSimpleName())
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to bind properties string", e);
        }
    }

    // ✅ 重载：不带 prefix → 绑定整个 map（适合无前缀的扁平配置）
    public static <T> T bind(String propsContent, Class<T> targetType) {
        return bind(propsContent, targetType, "");
    }

}