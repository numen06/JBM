package com.jbm.micro.mysql.web;

import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.micro.mysql.mp.MdExtendFormDefinition;
import com.jbm.micro.mysql.service.FormDesignerService;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模拟「动态表单编辑」微服务：新增/修改表单定义先入库，再发布到 Redis。
 */
@RestController
@RequestMapping("/api/h2/form-designer/forms")
public class FormDesignerController {

    private final FormDesignerService formDesignerService;

    public FormDesignerController(FormDesignerService formDesignerService) {
        this.formDesignerService = formDesignerService;
    }

    /**
     * 新建或更新表单（入库 + 发布 Redis）。formCode 走路径，避免 Fastjson 对内嵌 DTO 绑定不稳定。
     */
    @PostMapping("/{formCode}")
    public ResultBody<MdExtendFormDefinition> save(
            @PathVariable String formCode,
            @RequestBody SaveFormRequest request) {
        MdExtendFormDefinition saved = formDesignerService.saveAndPublish(
                formCode,
                request.getFormName(),
                request.getFields());
        return ResultBody.ok(saved);
    }

    /**
     * 仅更新字段并重新发布（模拟在线编辑表单）。
     */
    @PutMapping("/{formCode}")
    public ResultBody<MdExtendFormDefinition> update(
            @PathVariable String formCode,
            @RequestBody UpdateFormRequest request) {
        MdExtendFormDefinition saved = formDesignerService.saveAndPublish(
                formCode,
                request.getFormName(),
                request.getFields());
        return ResultBody.ok(saved);
    }

    /**
     * 从库重新发布到 Redis（Redis 清空或过期后的恢复）。
     */
    @PostMapping("/{formCode}/publish")
    public ResultBody<Boolean> publish(@PathVariable String formCode) {
        formDesignerService.publishToRedis(formCode);
        return ResultBody.ok(true);
    }

  /** 设计器读库（真源） */
    @GetMapping("/{formCode}")
    public ResultBody<MdExtendFormDefinition> getFromDb(@PathVariable String formCode) {
        return ResultBody.ok(formDesignerService.getByFormCode(formCode));
    }

    @Data
    public static class SaveFormRequest {
        private String formName;
        private List<FieldDefinition> fields;
    }

    @Data
    public static class UpdateFormRequest {
        private String formName;
        private List<FieldDefinition> fields;
    }
}
