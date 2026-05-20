package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 权限控制 API
 */
public interface IBaseAuthorityServiceClient {

    @GetMapping("/resources")
    ResultBody<List<AuthorityResource>> findAuthorityResource();

    @GetMapping("/menus")
    ResultBody<List<AuthorityMenu>> findAuthorityMenu();
}
