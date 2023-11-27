package com.jbm.cluster.doc.controller;

import cn.hutool.core.io.IoUtil;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.doc.model.Token;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.cluster.doc.service.WpsFileService;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.minio.MinioException;
import jbm.framework.web.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-18 08:28
 **/
@Api(tags = "文档资源管理")
@RestController
@RequestMapping("/")
@Slf4j
public class DocumentController {

    @Autowired
    private BaseDocService baseDocService;

    @Autowired
    private WpsFileService wpsFileService;

//    @ApiOperation(value = "获取文档列表")
//    @GetMapping("/list/{filePath}")
//    public ResultBody list(@PathVariable("filePath") String filePath, HttpServletRequest request) throws MinioException {
////        final String filePath = getExtractPath(request);
//        return ResultBody.ok().data(minioService.getFullList(Paths.get(filePath)));
//    }

//    public static void main(String[] args) {
//        System.out.println(FileNameUtil.getPath("/test"));
//        System.out.println(FileNameUtil.getPath("/test/"));
//        System.out.println(FileNameUtil.getPath("/test/testwet/hsdad.xtew"));
//    }

    /**
     * 获取处理后的请求路径
     *
     * @param request 请求对象
     * @return 处理后的请求路径
     */
    private static String getExtractPath(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }

    /**
     * 上传随机文档
     *
     * @param file   上传的文件
     * @param request 请求对象
     * @return 上传结果
     */
    @ApiOperation(value = "上传随机文档")
    @PostMapping("/put")
    public ResultBody<String> put(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) {
        return ResultBody.callback("上传文档成功", new Supplier<String>() {
            @Override
            public String get() {
                return baseDocService.uploadDoc(file, request).getDocPath();
            }
        });
    }

    /**
     * 上传特定文档
     *
     * @param file   上传的文件
     * @param request 请求对象
     * @return 上传结果
     */
    @ApiOperation(value = "上传特定文档")
    @PostMapping("/upload")
    public ResultBody<String> upload(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) {
        return ResultBody.callback("上传文档成功", new Supplier<String>() {
            @Override
            public String get() {
                return baseDocService.uploadDoc(file, request).getDocPath();
            }
        });
    }

    /**
     * 删除文档
     *
     * @param filePath 文档路径
     * @param request  请求对象
     * @return 删除结果
     */
    @ApiOperation(value = "删除文档")
    @GetMapping("/remove/{filePath}")
    public ResultBody<Void> remove(@PathVariable("filePath") String filePath, HttpServletRequest request) {
        return ResultBody.callback("删除文档成功", new Supplier<Void>() {
            @Override
            public Void get() {
                baseDocService.removeDoc(filePath);
                return null;
            }
        });
    }

    /**
     * 获取一个文档
     *
     * @param filePath 文档路径
     * @param request  请求对象
     * @param response 响应对象
     * @throws MinioException       MinIO异常
     * @throws IOException          IO异常
     */
    @ApiOperation(value = "获取一个文档")
    @GetMapping("/get/{filePath}")
    public void get(@PathVariable("filePath") String filePath, HttpServletRequest request, HttpServletResponse response) throws MinioException, IOException {
//        filePath = getExtractPath(request);
        BaseDoc baseDoc = new BaseDoc();
        baseDoc.setDocPath(filePath);
        InputStream inputStream = baseDocService.getDoc(baseDoc);
        response.setContentLength(baseDoc.getSize().intValue());
        WebUtils.setFileInlineHeader(response, baseDoc.getDocName());
        response.setContentType(baseDoc.getContentType());
        IoUtil.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 下载一个文档
     *
     * @param filePath 文档路径
     * @param request  请求对象
     * @param response 响应对象
     * @throws MinioException       MinIO异常
     * @throws IOException          IO异常
     */
    @ApiOperation(value = "下载一个文档")
    @GetMapping("/download/{filePath}")
    public void download(@PathVariable("filePath") String filePath, HttpServletRequest request, HttpServletResponse response) throws MinioException, IOException {
//        final String filePath = getExtractPath(request);
        BaseDoc baseDoc = new BaseDoc();
        baseDoc.setDocPath(filePath);
        InputStream inputStream = baseDocService.getDoc(baseDoc);
        response.setContentLength(baseDoc.getSize().intValue());
        WebUtils.setFileDownloadHeader(response, baseDoc.getDocName());
        response.setContentType(baseDoc.getContentType());
//        response.setContentType(baseDoc.getContentType());
        IoUtil.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 获取网络文件预览URL
     *
     * @param fileUrl fileUrl
     * @return t
     */
    @GetMapping("getViewUrl")
    public ResultBody<Token> getViewUrlWebPath(String fileUrl) {
        log.info("getViewUrlWebPath：fileUrl={}", fileUrl);
        Token t = wpsFileService.getViewUrl(fileUrl, true);
        return ResultBody.ok().data(t);
    }

}
