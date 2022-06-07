package com.jbm.cluster.doc.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import com.google.common.base.Charsets;
import com.jbm.framework.metadata.bean.ResultBody;
import io.minio.StatObjectResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.minio.MinioException;
import jbm.framework.boot.autoconfigure.minio.MinioService;
import jodd.io.FileNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @ApiOperation(value = "获取文档列表")
    @GetMapping("/list/{filePath}")
    public ResultBody list(@PathVariable("filePath") String filePath, HttpServletRequest request) throws MinioException {
//        final String filePath = getExtractPath(request);
        return ResultBody.ok().data(minioService.getFullList(Paths.get(filePath)));
    }

//    public static void main(String[] args) {
//        System.out.println(FileNameUtil.getPath("/test"));
//        System.out.println(FileNameUtil.getPath("/test/"));
//        System.out.println(FileNameUtil.getPath("/test/testwet/hsdad.xtew"));
//    }


    @ApiOperation(value = "上传随机文档")
    @PostMapping("/put")
    public ResultBody<String> put(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) {
        try {
            if (file == null) {
                throw new NullPointerException("上传文件为空");
            }
            final String filePath = getExtractPath(request);
            //扩展名
            String extName = FileUtil.extName(file.getOriginalFilename());
            //随机名称
            String mainName = IdUtil.objectId();
            String fileName = mainName + "." + extName;
            Path result = Paths.get(filePath, fileName);
            minioService.upload(result, file.getInputStream(), file.getContentType());
            return ResultBody.ok().
                    data(FileNameUtil.normalize(result.toString(), true)).
                    msg("上传文档成功");
        } catch (Exception e) {
            return ResultBody.failed().msg("上传文档失败");
        }
    }

    @ApiOperation(value = "上传特定文档")
    @PostMapping("/upload")
    public ResultBody<String> upload(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) {
        try {
            if (file == null) {
                throw new NullPointerException("上传文件为空");
            }
            final String filePath = getExtractPath(request);
            String fileName = file.getOriginalFilename();

            Path result = Paths.get(filePath, fileName);
            minioService.upload(result, file.getInputStream(), file.getContentType());
            return ResultBody.ok().
                    data(FileNameUtil.normalize(result.toString(), true)).
                    msg("上传文档成功");
        } catch (Exception e) {
            return ResultBody.failed().msg("上传文档失败");
        }
    }


    @ApiOperation(value = "删除文档")
    @GetMapping("/remove/{filePath}")
    public ResultBody remove(@PathVariable("filePath") String filePath, HttpServletRequest request) {
        try {
//            final String filePath = getExtractPath(request);
            minioService.remove(Paths.get(filePath));
            return ResultBody.ok().data(FileNameUtil.normalize(filePath, true)).msg("删除文档成功");
        } catch (Exception e) {
            return ResultBody.failed().msg("删除文档失败");
        }
    }

    @ApiOperation(value = "获取一个文档")
    @GetMapping("/get/{filePath}")
    public void get(@PathVariable("filePath") String filePath, HttpServletRequest request, HttpServletResponse response) throws MinioException, IOException {
//        filePath = getExtractPath(request);
        InputStream inputStream = minioService.get(Paths.get(filePath));
        // Set the content type and attachment header.
        String fileName = FileNameUtil.getName(filePath);
        fileName = HttpUtil.encodeParams(fileName, Charsets.UTF_8);
        StatObjectResponse statObjectResponse = minioService.getMetadata(Paths.get(filePath));
        response.setContentLength(Long.valueOf(statObjectResponse.size()).intValue());
        response.addHeader("Content-disposition", "inline;filename=" + fileName);
        response.setContentType(URLConnection.guessContentTypeFromName(filePath));
        // Copy the stream to the response's output stream.
        IOUtils.copyLarge(inputStream, response.getOutputStream());
        response.flushBuffer();
    }


    @ApiOperation(value = "下载一个文档")
    @GetMapping("/download/{filePath}")
    public void download(@PathVariable("filePath") String filePath, HttpServletRequest request, HttpServletResponse response) throws MinioException, IOException {
//        final String filePath = getExtractPath(request);
        InputStream inputStream = minioService.get(Paths.get(filePath));
        // Set the content type and attachment header.
        String fileName = FileNameUtil.getName(filePath);
        fileName = HttpUtil.encodeParams(fileName, Charsets.UTF_8);
        StatObjectResponse statObjectResponse = minioService.getMetadata(Paths.get(filePath));
        response.setContentLength(Long.valueOf(statObjectResponse.size()).intValue());
        response.addHeader("Content-disposition", "attachment;filename=" + fileName);
        response.setContentType(URLConnection.guessContentTypeFromName(filePath));
        // Copy the stream to the response's output stream.
        IOUtils.copyLarge(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    private static String getExtractPath(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }

}
