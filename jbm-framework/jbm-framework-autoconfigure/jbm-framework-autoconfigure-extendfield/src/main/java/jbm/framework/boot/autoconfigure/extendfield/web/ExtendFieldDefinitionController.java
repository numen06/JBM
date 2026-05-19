package jbm.framework.boot.autoconfigure.extendfield.web;

import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionWriter;
import jbm.framework.boot.autoconfigure.extendfield.service.LocalFieldDefinitionService;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展字段定义管理（写入 Redis 元数据，业务数据仍在 MySQL）。
 */
@RestController
@RequestMapping("/api/extend-field")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExtendFieldDefinitionController {

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Autowired(required = false)
    private FieldDefinitionWriter fieldDefinitionWriter;

    @PostMapping("/definitions")
    public ResultBody<Boolean> saveDefinitions(@RequestBody SaveDefinitionsRequest request) {
        if (fieldDefinitionWriter == null) {
            return ResultBody.<Boolean>failed().msg("Redis 未配置，无法保存字段定义");
        }
        List<FieldDefinition> normalized = new ArrayList<>();
        if (request.getDefinitions() != null) {
            for (Object raw : request.getDefinitions()) {
                FieldDefinition def = LocalFieldDefinitionService.toFieldDefinition(raw);
                if (def != null) {
                    normalized.add(def);
                }
            }
        }
        fieldDefinitionWriter.saveFieldDefinitions(request.getFormCode(), normalized);
        return ResultBody.ok(true);
    }

    @GetMapping("/definitions/{formCode}")
    public ResultBody<List<FieldDefinition>> listDefinitions(@PathVariable String formCode) {
        if (fieldDefinitionService == null) {
            return ResultBody.<List<FieldDefinition>>failed().msg("字段定义服务未就绪");
        }
        return ResultBody.ok(fieldDefinitionService.getFieldDefinitions(formCode));
    }

    @Data
    public static class SaveDefinitionsRequest {
        private String formCode;
        /** Fastjson 反序列化后元素可能为 {@link com.alibaba.fastjson.JSONObject} */
        private List<?> definitions;
    }
}
