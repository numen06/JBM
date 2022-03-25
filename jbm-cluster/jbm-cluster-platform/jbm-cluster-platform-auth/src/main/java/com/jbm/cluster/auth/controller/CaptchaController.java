package com.jbm.cluster.auth.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Api(tags = "验证码中心")
@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String getVcodePath(String scope) {
        String key = "/vcode/" + StrUtil.emptyToDefault(scope, "");
        return key;
    }

    /**
     * 生成图像验证码
     *
     * @param response response请求对象
     * @throws IOException
     */
    @ApiOperation(value = "生成图像验证码", notes = "")
    @GetMapping(value = "/vcode.png")
    public void vcode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/png");
        //定义图形验证码的长和宽
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100, 4, 100);
//        request.getSession().setAttribute("vcode", lineCaptcha.getCode());
        stringRedisTemplate.opsForValue().set(this.getVcodePath(null), lineCaptcha.getCode(), 1, TimeUnit.MINUTES);
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
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100, 4, 100);
//        request.getSession().setAttribute("vcode", lineCaptcha.getCode());
        stringRedisTemplate.opsForValue().set(this.getVcodePath(scope), lineCaptcha.getCode(), 1, TimeUnit.MINUTES);
        //LineCaptcha lineCaptcha = new LineCaptcha(200, 100, 4, 150);
        //图形验证码写出，可以写出到文件，也可以写出到流
        //验证图形验证码的有效性，返回boolean值
        lineCaptcha.write(response.getOutputStream());
    }

    @ApiOperation(value = "对比验证码", notes = "")
    @GetMapping(value = "/{scope}/verify")
    public Boolean verify(@PathVariable(value = "scope") String scope, @RequestParam(required = false) String vcode, HttpServletRequest request) throws IOException {
        String checkCode = stringRedisTemplate.opsForValue().get(this.getVcodePath(scope));
        if (StrUtil.isBlank(vcode)) {
            return false;
        }
        if (vcode.equalsIgnoreCase(checkCode)) {
            return true;
        }
        return false;
    }
}
