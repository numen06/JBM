package com.jbm.cluster.doc.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
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

    /**
     * 上传随机文档
     *
     * @param file    上传的文件
     * @param request 请求对象
     * @return 上传结果
     */
    @ApiOperation(value = "上传随机文档")
    @PostMapping("/put")
    public ResultBody<String> put(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) {
        return ResultBody.callback("上传文档成功", new Supplier<String>() {
            @Override
            public String get() {
                return baseDocService.uploadDoc(file, null, request).getDocPath();
            }
        });
    }


    /**
     * 上传特定文档
     *
     * @param file    上传的文件
     * @param request 请求对象
     * @return 上传结果
     */
    @ApiOperation(value = "上传特定文档")
    @PostMapping("/upload")
    public ResultBody<String> upload(@RequestParam(value = "file", required = false) MultipartFile file, @RequestParam(value = "group", required = false) String group, HttpServletRequest request) {
        return ResultBody.callback("上传文档成功", new Supplier<String>() {
            @Override
            public String get() {
                BaseDoc baseDoc = new BaseDoc();
                baseDoc.setDocGroup(group);
                return baseDocService.uploadDoc(file, baseDoc, request).getDocPath();
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


    @ApiOperation(value = "删除文档")
    @GetMapping("/remove/{group}/{filePath}")
    public ResultBody<Void> remove(@PathVariable("filePath") String filePath, @PathVariable("group") String group, HttpServletRequest request) {
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
     * @throws MinioException MinIO异常
     * @throws IOException    IO异常
     */
    @ApiOperation(value = "获取一个文档")
    @GetMapping("/get/{filePath}")
    public void get(@PathVariable("filePath") String filePath, HttpServletRequest request, HttpServletResponse response) throws MinioException, IOException {
//        filePath = getExtractPath(request);
        BaseDoc baseDoc = new BaseDoc();
//        baseDoc.setDocId(FileNameUtil.getBaseName(filePath));
        baseDoc.setDocPath(filePath);
        InputStream inputStream = baseDocService.getDoc(baseDoc);
        response.setContentLength(baseDoc.getSize().intValue());
        WebUtils.setFileInlineHeader(response, baseDoc.getDocName());
        response.setContentType(baseDoc.getContentType());
        IoUtil.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 获取一个文档
     *
     * @param filePath 文档路径
     * @param request  请求对象
     * @param response 响应对象
     * @throws MinioException MinIO异常
     * @throws IOException    IO异常
     */
    @ApiOperation(value = "获取一个文档")
    @GetMapping("/get/{group}/{filePath}")
    public void get(@PathVariable("filePath") String filePath, @PathVariable("group") String group, HttpServletRequest request, HttpServletResponse response) throws MinioException, IOException {
        // 获取文档路径
        // filePath = getExtractPath(request);
        BaseDoc baseDoc = new BaseDoc();
        // 设置文档路径
        baseDoc.setDocPath(Paths.get(StrUtil.nullToDefault(group, ""), filePath).toString());
        // 获取文档输入流
        InputStream inputStream = baseDocService.getDoc(baseDoc);
        // 设置文档大小
        response.setContentLength(baseDoc.getSize().intValue());
        // 设置文档名称
        WebUtils.setFileInlineHeader(response, baseDoc.getDocName());
        // 设置文档内容类型
        response.setContentType(baseDoc.getContentType());
        // 复制输入流到输出流
        IoUtil.copy(inputStream, response.getOutputStream());
        // 刷新响应缓冲区
        response.flushBuffer();
    }



    /**
     * 下载一个文档
     *
     * @param filePath 文档路径
     * @param request  请求对象
     * @param response 响应对象
     * @throws MinioException MinIO异常
     * @throws IOException    IO异常
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
