package com.jbm.cluster.doc.service;

import com.alibaba.fastjson.JSON;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.messages.NotificationRecords;
import jbm.framework.boot.autoconfigure.minio.notification.MinioNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-18 08:50
 **/
@Service
@Slf4j
public class MinioNotificationService {

    @Autowired
    private MinioClient minioClient;

    private String DEF_BUCKET_NAME = "doc";

    @PostConstruct
    public void init() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(DEF_BUCKET_NAME).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(DEF_BUCKET_NAME).build());
            }
        } catch (Exception e) {
            log.error("创建Bucket失败", e);
        }

    }


    @MinioNotification({"s3:ObjectCreated:CompleteMultipartUpload"})
    public void handleUpload(NotificationRecords notificationInfo) {
        log.info("文件上传{}", JSON.toJSONString(notificationInfo));
    }


    @MinioNotification({"s3:ObjectRemoved:Delete"})
    public void handleDelete(NotificationRecords notificationInfo) {
        log.info("文件删除{}", JSON.toJSONString(notificationInfo));
    }


    @MinioNotification({"s3:ObjectAccessed:Get"})
    public void handleGet(NotificationRecords notificationInfo) {
        log.info("文件下载{}", JSON.toJSONString(notificationInfo));
    }
}
