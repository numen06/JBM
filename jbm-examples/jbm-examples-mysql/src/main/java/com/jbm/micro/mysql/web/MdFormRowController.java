package com.jbm.micro.mysql.web;

import com.jbm.micro.mysql.mp.MdFormRow;
import com.jbm.micro.mysql.service.MdFormRowService;
import com.jbm.micro.mysql.web.dto.CreateMdFormRowRequest;
import com.jbm.micro.mysql.web.dto.MdFormRowResponse;
import com.jbm.micro.mysql.web.dto.UpdateMdFormRowRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态 JSON 字段（{@code md_form_row.payload}）的完整 CRUD；入参/出参均为 DTO，不直接暴露实体给 HTTP。
 */
@RestController
@RequestMapping("/api/h2/mp/form-rows")
public class MdFormRowController {

    private final MdFormRowService mdFormRowService;

    public MdFormRowController(MdFormRowService mdFormRowService) {
        this.mdFormRowService = mdFormRowService;
    }

    @GetMapping
    public List<MdFormRowResponse> list() {
        return mdFormRowService.list().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MdFormRowResponse> get(@PathVariable Long id) {
        MdFormRow row = mdFormRowService.getById(id);
        if (row == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(row));
    }

    @PostMapping
    public MdFormRowResponse create(@RequestBody CreateMdFormRowRequest req) {
        MdFormRow row = new MdFormRow();
        row.setPayload(req.getPayload() != null ? req.getPayload() : Collections.emptyMap());
        mdFormRowService.save(row);
        return toResponse(row);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MdFormRowResponse> update(@PathVariable Long id, @RequestBody UpdateMdFormRowRequest req) {
        MdFormRow row = mdFormRowService.getById(id);
        if (row == null) {
            return ResponseEntity.notFound().build();
        }
        row.setPayload(req.getPayload() != null ? req.getPayload() : Collections.emptyMap());
        mdFormRowService.updateById(row);
        MdFormRow loaded = mdFormRowService.getById(id);
        return ResponseEntity.ok(toResponse(loaded));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!mdFormRowService.removeById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private MdFormRowResponse toResponse(MdFormRow e) {
        MdFormRowResponse r = new MdFormRowResponse();
        r.setId(e.getId());
        r.setPayload(e.getPayload() != null ? e.getPayload() : Collections.emptyMap());
        return r;
    }
}
