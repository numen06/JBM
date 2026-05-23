package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface IBaseApiKeyFeignClient {

    @GetMapping(params = "apiKey")
    BaseApiKey getByApiKey(@RequestParam("apiKey") String apiKey);

    @GetMapping("/{keyId}")
    BaseApiKey getByKeyId(@PathVariable("keyId") Long keyId);

    @GetMapping("/{keyId}/check")
    Boolean checkAuthority(@PathVariable("keyId") Long keyId, @RequestParam("apiId") Long apiId);
}
