package com.jbm.micro.mysql.web;

import com.jbm.micro.mysql.mp.MdSample;
import com.jbm.micro.mysql.service.MdSampleService;
import com.jbm.micro.mysql.web.dto.CreateMdSampleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/h2/mp/samples")
public class MdSampleController {

    private final MdSampleService mdSampleService;

    public MdSampleController(MdSampleService mdSampleService) {
        this.mdSampleService = mdSampleService;
    }

    @GetMapping
    public List<MdSample> list() {
        return mdSampleService.list();
    }

    @PostMapping
    public MdSample create(@RequestBody CreateMdSampleRequest req) {
        MdSample e = new MdSample();
        e.setName(req.getName());
        e.setFormJson(req.getFormJson());
        mdSampleService.save(e);
        return e;
    }
}
