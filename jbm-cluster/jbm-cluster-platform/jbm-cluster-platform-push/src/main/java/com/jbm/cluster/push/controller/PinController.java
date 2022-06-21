package com.jbm.cluster.push.controller;

import cn.hutool.core.lang.Validator;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.push.service.PinService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.ObjectUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("验证码接口")
@RestController
@RequestMapping("/pin")
public class PinController {
    @Autowired
    private PinService pinService;

    /**
     * @Description: 获取短信验证码
     * @Author: wesley.zhang
     * @Date: 2018/12/13
     **/
    @ApiOperation("发送验证码")
    @GetMapping("/send")
    public ResultBody sendCode(String phoneNumber) {
        try {
            if (!Validator.isMobile(phoneNumber)) {
                return ResultBody.error(null, "手机号码错误");
            }
            JSONObject ret = pinService.sendPinCode(phoneNumber);
            return ResultBody.success(ret, "短信验证码发送成功");
        } catch (ServiceException e) {
            return ResultBody.error(e);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    /**
     * 验证，验证码
     *
     * @param phoneNumber
     * @return
     */
    @ApiOperation("判断验证是否正确")
    @PostMapping("/vif")
    public ResultBody vidCode(String phoneNumber, String code) {
        if (ObjectUtils.isNull(code)) {
            return ResultBody.error(null, "验证码错误");
        }
        if (!Validator.isMobile(phoneNumber)) {
            return ResultBody.error(null, "手机号码错误");
        }
        try {
            Boolean ret = pinService.vifCode(phoneNumber, code);
            return ResultBody.success(ret, ret ? "成功" : "错误");
        } catch (Exception e) {
            return ResultBody.error(null, "校验失败");
        }
    }


}
