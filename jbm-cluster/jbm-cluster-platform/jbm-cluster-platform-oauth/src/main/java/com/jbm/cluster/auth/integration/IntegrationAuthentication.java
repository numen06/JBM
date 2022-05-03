package com.jbm.cluster.auth.integration;

import lombok.Data;

import java.util.Map;

/**
 * @author wesley.zhang
 * @date 2018-3-30
 **/
@Data
public class IntegrationAuthentication {

    private String loginType;
    private String username;
    private Map<String, String[]> authParameters;

    public String getAuthParameter(String paramter) {
        String[] values = this.authParameters.get(paramter);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }
}
