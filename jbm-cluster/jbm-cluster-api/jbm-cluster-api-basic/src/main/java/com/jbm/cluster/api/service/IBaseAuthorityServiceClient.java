package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 权限控制API接口
 *
 * @author wesley.zhang
 */
public interface IBaseAuthorityServiceClient {
    /**
     * 获取所有访问权限列表
     *
     * @return
     */
    @GetMapping("/access")
    ResultBody<List<AuthorityResource>> findAuthorityResource();

    /**
     * 获取菜单权限列表
     *
     * @return
     */
    @GetMapping("/menu")
    ResultBody<List<AuthorityMenu>> findAuthorityMenu();
}
