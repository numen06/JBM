package com.jbm.cluster.system.gateway.server.controller;

import com.jbm.cluster.system.gateway.server.configuration.ApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author: wesley.zhang
 * @date: 2018/11/5 16:33
 * @description:
 */
@Controller
public class IndexController {
    @Autowired
    private ApiProperties apiProperties;
    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Value("${spring.application.name}")
    private String serviceId;

    @GetMapping("/")
    public String index() {
        if (apiProperties.getApiDebug()) {
            return "redirect:doc.html";
        }
        return "index";
    }
}
