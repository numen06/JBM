package com.jbm.cluster.common.satoken.standardjwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard OAuth2/OIDC JWT verification settings for Python auth compatibility.
 */
@Data
@ConfigurationProperties(prefix = "jbm.security.standard-jwt")
public class StandardJwtProperties {

    /**
     * Disabled by default so existing Sa-Token behavior is unchanged.
     */
    private boolean enabled = false;

    /**
     * Expected issuer. Empty means skip issuer validation.
     */
    private String issuer;

    /**
     * Expected audience. Empty means skip audience validation.
     */
    private String audience;

    /**
     * JWKS endpoint exposed by the standard OAuth/OIDC auth service.
     */
    private String jwksUri;

    /**
     * PEM public key fallback for RS256/RS384/RS512 tokens.
     */
    private String publicKey;

    /**
     * Shared secret fallback for HS256/HS384/HS512 tokens, mainly for local tests.
     */
    private String sharedSecret;

    /**
     * Allowed clock skew in seconds for exp/nbf.
     */
    private long clockSkewSeconds = 60;

    /**
     * JWKS cache time in seconds.
     */
    private long jwksCacheSeconds = 300;

    private String loginIdClaim = "sub";

    private String userIdClaim = "user_id";

    private String usernameClaim = "username";

    private String clientIdClaim = "client_id";

    private String tenantIdClaim = "tenant_id";

    private String appIdClaim = "app_id";

    private String userTypeClaim = "user_type";

    private String permissionsClaim = "permissions";

    private String rolesClaim = "roles";

    private List<String> acceptedAlgorithms = new ArrayList<String>();
}
