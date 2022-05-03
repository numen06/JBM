package com.jbm.cluster.api.model.api;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.Serializable;
import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/4/30 17:17
 * @Description TODO
 */
@Data
@Builder
public class JbmApi implements Serializable {

    /**
     *
     */
    private String apiName;
    private String apiCode;
    private String apiDesc;
    private Set<String> paths;
    private String className;
    private Set<RequestMethod> requestMethods;
    private String md5;
    private String requestMethod;
    private String serviceId;
    private Set<MediaType> contentTypes;
    private Boolean isAuth;
    private String permissions;


}
