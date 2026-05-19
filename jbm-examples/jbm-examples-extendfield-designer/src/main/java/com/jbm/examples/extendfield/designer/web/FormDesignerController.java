package com.jbm.examples.extendfield.designer.web;

import com.jbm.examples.extendfield.designer.mp.MdExtendFormDefinition;
import com.jbm.examples.extendfield.designer.service.FormDesignerService;
import com.jbm.framework.metadata.bean.ResultBody;
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

@RestController
@RequestMapping("/api/designer/forms")
public class FormDesignerController {

    private final FormDesignerService formDesignerService;

    public FormDesignerController(FormDesignerService formDesignerService) {
        this.formDesignerService = formDesignerService;
    }

    @PostMapping("/{formCode}")
    public ResultBody<MdExtendFormDefinition> save(
            @PathVariable String formCode,
            @RequestBody SaveFormRequest request) {
        return ResultBody.ok(formDesignerService.saveAndPublish(
                formCode, request.getFormName(), request.getFields()));
    }

    @PutMapping("/{formCode}")
    public ResultBody<MdExtendFormDefinition> update(
            @PathVariable String formCode,
            @RequestBody UpdateFormRequest request) {
        return ResultBody.ok(formDesignerService.saveAndPublish(
                formCode, request.getFormName(), request.getFields()));
    }

    @PostMapping("/{formCode}/publish")
    public ResultBody<Boolean> publish(@PathVariable String formCode) {
        formDesignerService.publishToRedis(formCode);
        return ResultBody.ok(true);
    }

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
