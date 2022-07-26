package com.jbm.cluster.bigscreen.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.MessageFormat;

/**
 * @Author: wesley.zhang
 * @Create: 2021-09-03 17:08:07
 */
@Api(tags = "大屏跳转接口")
@Controller
@RequestMapping("/view")
public class ViewController {

    @ApiOperation(value = "预览大屏", notes = "预览大屏")
    @GetMapping("/{view}")
    public String view(@PathVariable String view) {
        if (StrUtil.isEmpty(view)) {
            return "没有指定的大屏";
        }
        return MessageFormat.format("redirect:/static/{0}/index.html", view);
    }

}
