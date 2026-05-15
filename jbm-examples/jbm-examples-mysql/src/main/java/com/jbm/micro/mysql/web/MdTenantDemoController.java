package com.jbm.micro.mysql.web;

import com.jbm.micro.mysql.mp.MdTenantDemo;
import com.jbm.micro.mysql.service.MdTenantDemoService;
import com.jbm.micro.mysql.web.dto.CreateMdTenantDemoRequest;
import com.jbm.micro.mysql.web.dto.MdTenantDemoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/h2/mp/tenant-demos")
public class MdTenantDemoController {

    private final MdTenantDemoService mdTenantDemoService;

    public MdTenantDemoController(MdTenantDemoService mdTenantDemoService) {
        this.mdTenantDemoService = mdTenantDemoService;
    }

    @GetMapping
    public List<MdTenantDemoResponse> list() {
        return mdTenantDemoService.list().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MdTenantDemoResponse> get(@PathVariable Long id) {
        MdTenantDemo row = mdTenantDemoService.getById(id);
        if (row == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(row));
    }

    @PostMapping
    public MdTenantDemoResponse create(@RequestBody CreateMdTenantDemoRequest req) {
        MdTenantDemo row = new MdTenantDemo();
        row.setName(req.getName());
        row.setRemark(req.getRemark());
        mdTenantDemoService.save(row);
        MdTenantDemo loaded = mdTenantDemoService.getById(row.getId());
        return toResponse(loaded != null ? loaded : row);
    }

    private MdTenantDemoResponse toResponse(MdTenantDemo e) {
        MdTenantDemoResponse r = new MdTenantDemoResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setTenantId(e.getTenantId());
        r.setRemark(e.getRemark());
        return r;
    }
}
