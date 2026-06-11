package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.PublishedApiDoc;
import com.jbm.cluster.common.mysql.service.PublishedApiDocService;
import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "公开 API 文档")
@RestController
@RequestMapping("/published-docs")
public class PublishedApiDocController extends BaseController {

    @Autowired
    private PublishedApiDocService publishedApiDocService;

    @PermitAll
    @ApiOperation("已发布文档列表")
    @GetMapping("/openapi")
    public ResultBody<List<Map<String, Object>>> listPublished() {
        return ResultBody.callback(() -> publishedApiDocService.listActive().stream()
                .map(this::toSummary)
                .collect(Collectors.toList()));
    }

    @PermitAll
    @ApiOperation("已发布文档 spec")
    @GetMapping(value = "/openapi/{docKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultBody<String> getPublishedSpec(@PathVariable String docKey) {
        return ResultBody.callback(() -> {
            PublishedApiDoc doc = publishedApiDocService.getByDocKey(docKey);
            if (doc == null || doc.getStatus() == null || doc.getStatus() != 1) {
                return null;
            }
            return doc.getPublishedSpec();
        });
    }

    private Map<String, Object> toSummary(PublishedApiDoc doc) {
        Map<String, Object> map = new HashMap<>(8);
        map.put("docKey", doc.getDocKey());
        map.put("title", doc.getTitle());
        map.put("version", doc.getVersion());
        map.put("contentType", doc.getContentType());
        map.put("publishedAt", doc.getPublishedAt());
        map.put("publishedSummary", doc.getPublishedSummary());
        map.put("url", "/published-docs/openapi/" + doc.getDocKey());
        return map;
    }
}
