package com.jbm.cluster.doc.controller;

import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.cluster.doc.service.DocFileReadinessService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 文档文件服务就绪探针，供大屏等服务确认 doc 能否提供文件下载。
 */
@Api(tags = "健康检查")
@RestController
@RequestMapping("/health")
@Slf4j
public class DocHealthController {

    @Autowired
    private DocFileReadinessService docFileReadinessService;

    @PermitAll
    @ApiOperation(value = "文件服务就绪探针")
    @GetMapping("/file")
    public ResponseEntity<Map<String, Object>> fileHealth() {
        if (docFileReadinessService.isFileServiceReady()) {
            return ResponseEntity.ok(Collections.singletonMap("ready", true));
        }
        log.warn("文档文件服务尚未就绪");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonMap("ready", false));
    }
}
