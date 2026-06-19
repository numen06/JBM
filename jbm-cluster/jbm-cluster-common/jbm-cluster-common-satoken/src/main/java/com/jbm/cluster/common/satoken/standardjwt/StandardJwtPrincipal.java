package com.jbm.cluster.common.satoken.standardjwt;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class StandardJwtPrincipal {

    private String token;

    private String loginId;

    private Map<String, Object> claims;

    private JbmLoginUser loginUser;
}
