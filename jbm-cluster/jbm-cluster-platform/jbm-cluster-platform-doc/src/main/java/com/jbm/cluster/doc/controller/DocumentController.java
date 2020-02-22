package com.jbm.cluster.doc.controller;

import com.jbm.cluster.common.security.OpenHelper;
import com.jbm.cluster.common.security.OpenUserDetails;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jlefebure.spring.boot.minio.MinioException;
import com.jlefebure.spring.boot.minio.MinioService;
import io.minio.messages.Item;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-18 08:28
 **/
@Api(tags = "文档资源管理")
@RestController
@RequestMapping("/")
@Slf4j
public class DocumentController {

    @Autowired
    private MinioService minioService;

    @GetMapping("/user")
    public ResultBody user() {
        OpenUserDetails openUesr = OpenHelper.getUser();
        return ResultBody.ok().data(openUesr);
    }

    @ApiOperation(value = "获取文档列表")
    @GetMapping("/list")
    public ResultBody list() throws MinioException {
        return ResultBody.ok().data(minioService.list());
    }

    @ApiOperation(value = "上传文档")
    @PostMapping("/upload")
    public ResultBody upload(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            minioService.upload(Paths.get(fileName), file.getInputStream(), file.getContentType());
            return ResultBody.ok().msg("上传文档成功");
        } catch (Exception e) {
            return ResultBody.failed().msg("上传文档失败");
        }
    }

    @ApiOperation(value = "删除文档")
    @GetMapping("/remove/{object}")
    public ResultBody remove(@PathVariable("object") String object) {
        try {
            minioService.remove(Paths.get(object));
            return ResultBody.ok().msg("删除文档成功");
        } catch (Exception e) {
            return ResultBody.failed().msg("删除文档失败");
        }
    }

    @ApiOperation(value = "获取一个文档")
    @GetMapping("/get/{object}")
    public void get(@PathVariable("object") String object, HttpServletResponse response) throws MinioException, IOException {
        InputStream inputStream = minioService.get(Paths.get(object));
        // Set the content type and attachment header.
        response.addHeader("Content-disposition", "attachment;filename=" + object);
        response.setContentType(URLConnection.guessContentTypeFromName(object));
        // Copy the stream to the response's output stream.
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }
}
