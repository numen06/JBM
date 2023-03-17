package com.jbm.test;

import io.minio.messages.Item;
import io.minio.messages.NotificationRecords;
import jbm.framework.boot.autoconfigure.minio.MinioException;
import jbm.framework.boot.autoconfigure.minio.MinioService;
import jbm.framework.boot.autoconfigure.minio.notification.MinioNotification;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-18 07:59
 **/
@RestController
@RequestMapping("/files")
public class TestController {

    @Autowired
    private MinioService minioService;


    @GetMapping("/")
    public List<Item> testMinio() throws MinioException {
        return minioService.list();
    }

    @GetMapping("/{object}")
    public void getObject(@PathVariable("object") String object, HttpServletResponse response) throws MinioException, IOException {
        InputStream inputStream = minioService.get(Paths.get(object));
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        // Set the content type and attachment header.
        response.addHeader("Content-disposition", "attachment;filename=" + object);
        response.setContentType(URLConnection.guessContentTypeFromName(object));

        // Copy the stream to the response's output stream.
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    @MinioNotification({"s3:ObjectAccessed:Get"})
    public void handleGet(NotificationRecords notificationInfo) {
        System.out.println("Minio Hello world");
    }
}
