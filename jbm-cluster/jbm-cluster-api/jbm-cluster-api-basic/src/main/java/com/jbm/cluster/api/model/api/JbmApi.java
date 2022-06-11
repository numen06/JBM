package com.jbm.cluster.api.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/4/30 17:17
 * @Description TODO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JbmApi implements Serializable {

    /**
     *
     */
    private String apiName;
    private String apiCode;
    private String apiDesc;
    private Set<String> paths;
    private String className;
    private Set<String> requestMethods;
    private String md5;
    private String requestMethod;
    private String serviceId;
    private Set<String> contentTypes;
    private Boolean isAuth;
    private String permissions;


}
