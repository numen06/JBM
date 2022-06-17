package com.jbm.cluster.auth.controller;

import cn.hutool.captcha.LineCaptcha;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.auth.service.BaseAppPreprocessing;
import com.jbm.cluster.auth.service.VCoderService;
import com.jbm.cluster.auth.service.feign.BaseAppServiceClient;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

@Api(tags = "验证码中心")
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private VCoderService vCoderService;
    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;

    @ApiOperation(value = "获取应用公钥", notes = "")
    @GetMapping(value = "/pkey")
    public ResultBody<String> getPKey(@RequestParam(required = true) String appKey) {
        return new ResultBody<String>().call(new Consumer<ResultBody<String>>() {
            @Override
            public void accept(ResultBody<String> stringResultBody) {
                BaseApp baseApp = baseAppPreprocessing.getAppByKey(appKey);
                String pkey = SecurityUtils.generateRSAPublicKey(baseApp.getSecretKey());
                stringResultBody.setResult(pkey);
            }
        });
    }

    /**
     * 生成图像验证码
     *
     * @param response response请求对象
     * @throws IOException
     */
    @ApiOperation(value = "生成图像验证码", notes = "")
    @GetMapping(value = "/vcode.png")
    public void vcode(@RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/png");
        //定义图形验证码的长和宽
        LineCaptcha lineCaptcha = vCoderService.build(null, width, height, 5);
        //LineCaptcha lineCaptcha = new LineCaptcha(200, 100, 4, 150);
        //图形验证码写出，可以写出到文件，也可以写出到流
        //验证图形验证码的有效性，返回boolean值
        lineCaptcha.write(response.getOutputStream());
    }

    /**
     * 生成图像验证码
     *
     * @param response response请求对象
     * @throws IOException
     */
    @ApiOperation(value = "生成图像验证码", notes = "")
    @GetMapping(value = "/{scope}/vcode.png")
    public void vcode(@PathVariable(value = "scope") String scope, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/png");
        //定义图形验证码的长和宽
        LineCaptcha lineCaptcha = vCoderService.build(scope, 200, 80, 5);
        //LineCaptcha lineCaptcha = new LineCaptcha(200, 100, 4, 150);
        //图形验证码写出，可以写出到文件，也可以写出到流
        //验证图形验证码的有效性，返回boolean值
        lineCaptcha.write(response.getOutputStream());
    }

    @ApiOperation(value = "对比验证码", notes = "")
    @GetMapping(value = "/{scope}/verify")
    public ResultBody<Boolean> verify(@PathVariable(value = "scope") String scope, @RequestParam(required = false) String vcode, HttpServletRequest request) throws IOException {
        return ResultBody.ok().data(vCoderService.verify(vcode, scope));
    }
}
