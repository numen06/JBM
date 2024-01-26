package com.jbm.cluster.auth.controller;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.Validator;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.auth.service.BaseAppPreprocessing;
import com.jbm.cluster.auth.service.PCoderService;
import com.jbm.cluster.auth.service.VCoderService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;

@Api(tags = "验证码中心")
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private VCoderService vCoderService;
    @Autowired
    private PCoderService pCoderService;
    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;

    @ApiOperation(value = "获取应用公钥", notes = "")
    @GetMapping(value = "/pkey")
    public ResultBody<String> getPKey(@RequestParam(required = true) String appKey) {
        return ResultBody.callback(new Supplier<String>() {
            @Override
            public String get() {
                BaseApp baseApp = baseAppPreprocessing.getAppByKey(appKey);
                return baseApp.getPublicKey();
            }
        });
    }

    @ApiOperation(value = "生成图像验证码BASE64形式")
    @GetMapping(value = "/vcode64")
    public ResultBody<String> vcode64(@RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //定义图形验证码的长和宽
        LineCaptcha lineCaptcha = vCoderService.build(null, width, height, 5);
        return ResultBody.ok().data(lineCaptcha.getImageBase64Data());
    }

    /**
     * 生成图像验证码
     *
     * @param response response请求对象
     * @throws IOException
     */
    @ApiOperation(value = "生成图像验证码", notes = "")
    @GetMapping(value = "/vcode.png")
    public void vcodepng(@RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height, HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    @ApiOperation(value = "对比验证码", notes = "")
    @GetMapping(value = "/verify")
    public ResultBody<Boolean> verify(@RequestParam(required = false) String vcode, HttpServletRequest request) throws IOException {
        return ResultBody.ok().data(vCoderService.verify(vcode, "system"));
    }

    @ApiOperation(value = "发送验证码")
    @GetMapping(value = "/pcode")
    public ResultBody<Boolean> pcode(@RequestParam(required = true) String phone, @RequestParam(required = true) String vcode) throws IOException {
        vCoderService.verify(vcode);
        Validator.validateMobile(phone, "非法手机号");
        pCoderService.send(phone);
        return ResultBody.success("验证码发送成功");
    }

    @ApiOperation(value = "对比手机验证码")
    @GetMapping(value = "/pcode/verify")
    public ResultBody<Boolean> pcodeVerify(@RequestParam(required = true) String phone, @RequestParam(required = true) String vcode) throws IOException {
        Validator.validateMobile(phone, "非法手机号");
        pCoderService.verify(vcode, phone);
        return ResultBody.success("手机验证码验证成功");
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


//    @Autowired
//    private CaptchaService captchaService;
//
//    @PostMapping("/blockPuzzle")
//    public ResultBody<Object> blockPuzzle(@RequestBody CaptchaVO captchaVO) {
//        ResponseModel responseModel = captchaService.get(captchaVO);
//        return ResultBody.ok().data(responseModel.getRepData());
//    }
//
//    @PostMapping("/checkBlock")
//    public ResultBody checkBlock(@RequestBody CaptchaVO captchaVO) {
//        ResponseModel response = captchaService.verification(captchaVO);
//        if (response.isSuccess() == false) {
//            ResultBody.failed().msg(response.getRepMsg());
//            //验证码校验失败，返回信息告诉前端
//            //repCode  0000  无异常，代表成功
//            //repCode  9999  服务器内部异常
//            //repCode  0011  参数不能为空
//            //repCode  6110  验证码已失效，请重新获取
//            //repCode  6111  验证失败
//            //repCode  6112  获取验证码失败,请联系管理员
//        }
//        return ResultBody.ok();
//    }


}
