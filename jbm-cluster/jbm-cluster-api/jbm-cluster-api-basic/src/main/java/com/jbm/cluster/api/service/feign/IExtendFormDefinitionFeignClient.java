package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import org.springframework.web.bind.annotation.*;
import java.util.List;

public interface IExtendFormDefinitionFeignClient {
    @PostMapping("/{formCode}")
    ExtendFormDefinition save(@PathVariable("formCode") String formCode, @RequestBody SaveExtendFormRequest request);
    @PutMapping("/{formCode}")
    ExtendFormDefinition update(@PathVariable("formCode") String formCode, @RequestBody SaveExtendFormRequest request);
    @PostMapping("/{formCode}/publish")
    Boolean publish(@PathVariable("formCode") String formCode);
    @GetMapping("/{formCode}")
    ExtendFormDefinition getFromDb(@PathVariable("formCode") String formCode);
    @GetMapping("/{formCode}/definitions")
    List<FieldDefinition> listFromRedis(@PathVariable("formCode") String formCode);
}