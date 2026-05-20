package jbm.framework.boot.autoconfigure.extendfield;

import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantResolver;
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
     * 是否注册框架自带 {@code /api/extend-field/definitions} 控制器（Center 等平台可关闭以避免路径重复）。
     */
    private boolean builtinDefinitionControllerEnabled = true;

    /**
     * 本地表单字段定义（formCode -> 字段列表），用于启动加载或 LOCAL 模式。
     * 启用多租户时 key 可为 {@code tenantId:formCode}。
     */
    private Map<String, FormDefinition> definitions = new LinkedHashMap<>();

    /**
     * 多租户：Redis 与 Advice 使用 {@code tenantId:formCode} 作用域。
     */
    private Tenant tenant = new Tenant();

    @Data
    public static class FormDefinition {
        private List<FieldDefinition> fields = new ArrayList<>();
    }

    @Data
    public static class Tenant {
        /**
         * 是否按租户隔离字段定义（请求头注入 {@link jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantContext}）。
         */
        private boolean enabled = false;

        /**
         * 租户 ID 请求头，与业务网关约定一致即可（示例常用 {@code X-Demo-Tenant-Id}）。
         */
        private String header = "X-Extend-Tenant-Id";

        /**
         * 未传租户头时使用的默认模块 ID（Redis 作用域 {@code 0:formCode}、库表 {@code tenant_id=0}）。
         */
        private String defaultTenantId = ExtendFieldTenantResolver.DEFAULT_MODULE_TENANT_ID;

        /**
         * 为 true 时：无租户头则自动使用 {@link #defaultTenantId}，与显式传租户行为一致。
         */
        private boolean useDefaultWhenMissing = true;
    }
}
