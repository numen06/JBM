package com.jbm.test.controller;

import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "测试Swagger")
@RequestMapping("/")
@RestController
public class TestContoller {

    /**
     * 获取用户基础信息
     *
     * @return
     */
    @ApiOperation(value = "测试GET")
    @GetMapping("/testGet")
    public ResultBody testGet() {
        return ResultBody.ok();
    }


    @ApiOperation(value = "测试POST")
    @PostMapping("/testPost")
    public ResultBody testPost() {
        return ResultBody.ok();
    }


    @ApiOperation(value = "测试上传文件识别")
    @PostMapping("/put")
    public ResultBody<String> put(@RequestParam(value = "file", required = false) MultipartFile file) {
        return ResultBody.ok();
    }
}