package com.jbm.cluster.api.model.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Date;

/**
 * 自定义已授权权限标识
 *
 * @author wesley.zhang
 */
@Data
public   class OpenAuthority implements Serializable {
    private static final long serialVersionUID = -4682269495406460314L;

    /**
     * 权限Id
     */
    private String authorityId;
    /**
     * 权限标识
     */
    private String authority;
    /**
     * 过期时间,用于判断权限是否已过期
     */
    private Date expireTime;

    /**
     * 权限所有者
     */
    private String owner;

    @JsonProperty("isExpired")
    public Boolean getIsExpired() {
        if (expireTime != null && System.currentTimeMillis() > expireTime.getTime()) {
            return true;
        }
        return false;
    }

    public OpenAuthority() {
    }

    public OpenAuthority(String authority) {
        Assert.hasText(authority, "A granted authority textual representation is required");
        this.authority = authority;
    }

    public OpenAuthority(String authority, Date expireTime) {
        Assert.hasText(authority, "A granted authority textual representation is required");
        this.authority = authority;
        this.expireTime = expireTime;
    }

    public OpenAuthority(String authorityId, String authority, Date expireTime, String owner) {
        this.authorityId = authorityId;
        this.authority = authority;
        this.expireTime = expireTime;
        this.owner = owner;
    }




}
