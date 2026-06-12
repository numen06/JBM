package com.jbm.cluster.doc.controller;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.minio.MinioConfigurationProperties;
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
 * Minio 存储就绪探针，供大屏等服务在加载资源前确认存储链路可用。
 */
@Api(tags = "健康检查")
@RestController
@RequestMapping("/health")
@Slf4j
public class MinioHealthController {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfigurationProperties minioConfigurationProperties;

    @ApiOperation(value = "Minio 就绪探针")
    @GetMapping("/minio")
    public ResponseEntity<Map<String, Object>> minioHealth() {
        String bucket = minioConfigurationProperties.getBucket();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (exists) {
                return ResponseEntity.ok(Collections.singletonMap("ready", true));
            }
            log.warn("Minio bucket [{}] 不存在", bucket);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("ready", false));
        } catch (Exception e) {
            log.debug("Minio 探针失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("ready", false));
        }
    }
}
