package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

public interface IBaseAuthorityFeignClient {
    @GetMapping("/resources")
    List<AuthorityResource> findAuthorityResource();
    @GetMapping("/menus")
    List<AuthorityMenu> findAuthorityMenu();
}