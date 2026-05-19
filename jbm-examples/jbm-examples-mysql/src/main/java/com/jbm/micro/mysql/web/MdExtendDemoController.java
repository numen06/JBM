package com.jbm.micro.mysql.web;

import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.micro.mysql.mp.MdExtendDemo;
import com.jbm.micro.mysql.service.MdExtendDemoService;
import com.jbm.micro.mysql.web.dto.CreateMdExtendDemoRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 扩展字段联调：写入可走 formCode 平铺；查询可走 extendQuery；响应经 {@code ResultBody} 自动展开 extendData。
 */
@RestController
@RequestMapping("/api/h2/mp/extend-demos")
public class MdExtendDemoController {

    private final MdExtendDemoService mdExtendDemoService;

    public MdExtendDemoController(MdExtendDemoService mdExtendDemoService) {
        this.mdExtendDemoService = mdExtendDemoService;
    }

    @GetMapping
    public ResultBody<List<MdExtendDemo>> list() {
        return ResultBody.ok(mdExtendDemoService.list());
    }

    @GetMapping("/{id}")
    public ResultBody<MdExtendDemo> get(@PathVariable Long id) {
        return ResultBody.ok(mdExtendDemoService.getById(id));
    }

  /**
   * 直接提交 extendData，或提交 formCode + 扩展字段平铺（由 Advice 拆分）。
   */
    @PostMapping
    public ResultBody<MdExtendDemo> create(@RequestBody CreateMdExtendDemoRequest req) {
        MdExtendDemo entity = new MdExtendDemo();
        entity.setBizCode(req.getBizCode());
        entity.setTitle(req.getTitle());
        entity.setExtendData(req.getExtendData());
        mdExtendDemoService.save(entity);
        return ResultBody.ok(entity);
    }

    /**
     * 按 bizCode + 扩展字段内存过滤（H2 演示；生产环境请用 CommonMapper 的 JSON 路径片段）。
     */
    @PostMapping("/search")
    public ResultBody<List<MdExtendDemo>> search(@RequestBody Map<String, Object> criteria) {
        String bizCode = criteria.get("bizCode") != null ? String.valueOf(criteria.get("bizCode")) : null;
        Object extendQuery = criteria.get("extendQuery");
        if (extendQuery == null) {
            extendQuery = criteria.get("extend");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> extendEq = extendQuery instanceof Map ? (Map<String, Object>) extendQuery : null;

        List<MdExtendDemo> all = mdExtendDemoService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MdExtendDemo>()
                        .eq(bizCode != null, "biz_code", bizCode));
        if (extendEq == null || extendEq.isEmpty()) {
            return ResultBody.ok(all);
        }
        List<MdExtendDemo> matched = new java.util.ArrayList<>();
        for (MdExtendDemo row : all) {
            if (row.getExtendData() == null) {
                continue;
            }
            boolean ok = true;
            for (Map.Entry<String, Object> e : extendEq.entrySet()) {
                Object actual = row.getExtendData().get(e.getKey());
                if (actual == null || !String.valueOf(actual).equals(String.valueOf(e.getValue()))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                matched.add(row);
            }
        }
        return ResultBody.ok(matched);
    }
}
