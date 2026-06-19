package com.jbm.cluster.auth.controller;

import com.jbm.cluster.common.satoken.standardjwt.StandardJwtIssuer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final StandardJwtIssuer jwtIssuer;

    public JwksController(StandardJwtIssuer jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    @GetMapping({"/jwks.json", "/.well-known/jwks.json"})
    public Map<String, Object> jwks() {
        return jwtIssuer.jwks();
    }
}
