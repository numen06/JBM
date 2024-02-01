package com.jbm.micro.sb.test.service;

import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/")
public class SpringTestController {


    @Resource
    private SpringBootService springBootService;

    @GetMapping("/test")
    @ResponseBody
    public ResultBody test() {
        log.info("收到请求");
        return ResultBody.success(springBootService.toString());
    }
}
