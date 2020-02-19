package com.jbm.cluster.doc.service;

import com.alibaba.fastjson.JSON;
import com.jlefebure.spring.boot.minio.notification.MinioNotification;
import io.minio.notification.NotificationInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-18 08:50
 **/
@Service
@Slf4j
public class MinioNotificationService {

    @MinioNotification({"s3:ObjectCreated:CompleteMultipartUpload"})
    public void handleUpload(NotificationInfo notificationInfo) {
        log.info("文件上传{}", JSON.toJSONString(notificationInfo));
    }


    @MinioNotification({"s3:ObjectRemoved:Delete"})
    public void handleDelete(NotificationInfo notificationInfo) {
        log.info("文件删除{}", JSON.toJSONString(notificationInfo));
    }


    @MinioNotification({"s3:ObjectAccessed:Get"})
    public void handleGet(NotificationInfo notificationInfo) {
        log.info("文件下载{}", JSON.toJSONString(notificationInfo));
    }
}
