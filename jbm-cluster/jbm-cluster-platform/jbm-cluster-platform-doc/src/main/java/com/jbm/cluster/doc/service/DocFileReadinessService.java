package com.jbm.cluster.doc.service;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import jbm.framework.boot.autoconfigure.minio.MinioConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文档文件服务能力检查，存储实现细节不对外暴露。
 */
@Service
@Slf4j
public class DocFileReadinessService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfigurationProperties minioConfigurationProperties;

    public boolean isFileServiceReady() {
        String bucket = minioConfigurationProperties.getBucket();
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            log.debug("文件服务就绪检查失败: {}", e.getMessage());
            return false;
        }
    }
}
