package com.jbm.cluster.api.model.auth;

import lombok.Data;

import java.io.Serializable;
import java.util.*;

/**
 * 自定义客户端信息
 *
 * @author: wesley.zhang
 * @date: 2019/5/30 18:07
 * @description:
 */
@Data
public class OpenClientDetails implements Serializable {

    private static final long serialVersionUID = -4888527753331687039L;


    private String clientId;

    private String clientSecret;

    private Set<String> scope = Collections.emptySet();

    private Set<String> resourceIds = Collections.emptySet();

    private Set<String> authorizedGrantTypes = Collections.emptySet();

    private Set<String> registeredRedirectUris;
    private Set<String> autoApproveScopes;

    private List<OpenAuthority> authorities = Collections.emptyList();

    private Integer accessTokenValiditySeconds;

    private Integer refreshTokenValiditySeconds;

    private Map<String, Object> additionalInformation = new LinkedHashMap<String, Object>();


}
