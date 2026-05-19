package jbm.framework.boot.autoconfigure.extendfield;

import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扩展字段配置：{@code jbm.extend-field.*}
 */
@Data
@ConfigurationProperties(prefix = "jbm.extend-field")
public class ExtendFieldProperties {

    /**
     * 是否启用扩展字段 AOP 与字段定义服务。
     */
    private boolean enabled = false;

    /**
     * 响应是否将 extendData 平铺到同层。
     */
    private boolean autoFlatten = true;

    /**
     * 字段定义来源。
     */
    private FieldDefinitionSource source = FieldDefinitionSource.REDIS;

    /**
     * 启动时是否将本地 definitions 同步到 Redis。
     */
    private boolean syncLocalToRedisOnStartup = true;

    /**
     * 本地表单字段定义（formCode -> 字段列表），用于启动加载或 LOCAL 模式。
     */
    private Map<String, FormDefinition> definitions = new LinkedHashMap<>();

    @Data
    public static class FormDefinition {
        private List<FieldDefinition> fields = new ArrayList<>();
    }
}
