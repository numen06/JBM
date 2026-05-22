package com.jbm.cluster.auth.service;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.auth.config.ApiLoginSecurityProperties;
import com.jbm.cluster.core.constant.ApiSecurityConstants;
import com.jbm.cluster.core.security.ApiSecurityUtils;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录密码必须 RSA 加密（强制）；仅 dev profile + 配置允许 demo 明文。
 */
@Component
public class LoginPasswordSecurityService {

    private static final String DEMO_CLIENT = "demo";

    @Autowired
    private ApiLoginSecurityProperties apiLoginSecurityProperties;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public void assertLoginPasswordEncrypted(String clientId, String password, HttpServletRequest request) {
        if (!ApiSecurityConstants.LOGIN_PASSWORD_ENCRYPT_REQUIRED) {
            return;
        }
        if (StrUtil.isBlank(password)) {
            throw new ServiceException(ErrorCode.PASSWORD_PLAINTEXT_DENIED.getCode(), "密码不能为空");
        }
        if (allowsPlaintextLogin(clientId)) {
            return;
        }
        String encryptedHeader = request != null
                ? request.getHeader(ApiSecurityConstants.PASSWORD_ENCRYPTED) : null;
        if (ApiSecurityUtils.isPasswordEncryptedHeader(encryptedHeader)
                && ApiSecurityUtils.looksLikeRsaCiphertext(password)) {
            return;
        }
        if (ApiSecurityUtils.looksLikeRsaCiphertext(password)) {
            return;
        }
        throw new ServiceException(ErrorCode.PASSWORD_PLAINTEXT_DENIED.getCode(), "密码必须加密传输");
    }

    public boolean allowsPlaintextLogin(String clientId) {
        if (!apiLoginSecurityProperties.isLoginPlaintextDevOnly()) {
            return false;
        }
        if (!DEMO_CLIENT.equals(clientId)) {
            return false;
        }
        return isDevProfile();
    }

    private boolean isDevProfile() {
        if (StrUtil.isBlank(activeProfiles)) {
            return false;
        }
        String lower = activeProfiles.toLowerCase();
        return lower.contains("dev") || lower.contains("local") || lower.contains("h2");
    }
}
