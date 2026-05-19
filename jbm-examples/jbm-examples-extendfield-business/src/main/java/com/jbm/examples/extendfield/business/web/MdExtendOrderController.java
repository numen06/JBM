package com.jbm.examples.extendfield.business.web;

import com.jbm.examples.extendfield.business.mp.MdExtendOrder;
import com.jbm.examples.extendfield.business.service.MdExtendOrderService;
import com.jbm.examples.extendfield.business.web.dto.CreateMdExtendOrderRequest;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/business/orders")
public class MdExtendOrderController {

    private final MdExtendOrderService mdExtendOrderService;

    public MdExtendOrderController(MdExtendOrderService mdExtendOrderService) {
        this.mdExtendOrderService = mdExtendOrderService;
    }

    @GetMapping
    public ResultBody<List<MdExtendOrder>> list() {
        return ResultBody.ok(mdExtendOrderService.list());
    }

    @GetMapping("/{id}")
    public ResultBody<MdExtendOrder> get(@PathVariable Long id) {
        return ResultBody.ok(mdExtendOrderService.getById(id));
    }

    @PostMapping
    public ResultBody<MdExtendOrder> create(@RequestBody CreateMdExtendOrderRequest req) {
        MdExtendOrder entity = new MdExtendOrder();
        entity.setOrderNo(req.getOrderNo());
        entity.setTitle(req.getTitle());
        entity.setExtendData(req.getExtendData());
        mdExtendOrderService.save(entity);
        return ResultBody.ok(entity);
    }
}
