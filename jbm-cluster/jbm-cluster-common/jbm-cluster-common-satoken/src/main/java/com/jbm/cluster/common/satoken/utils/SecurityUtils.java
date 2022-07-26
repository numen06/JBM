package com.jbm.cluster.common.satoken.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.basic.context.SecurityContextHolder;
import com.jbm.cluster.core.constant.JbmTokenConstants;
import jbm.framework.web.ServletUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyPair;

/**
 * 权限获取工具类
 *
 * @author wesley.zhang
 */
public class SecurityUtils {
    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return SecurityContextHolder.getUserId();
    }

    /**
     * 获取用户名称
     */
    public static String getUsername() {
        return SecurityContextHolder.getUserName();
    }

    /**
     * 获取用户key
     */
    public static String getUserKey() {
        return SecurityContextHolder.getUserKey();
    }

    /**
     * 获取登录用户信息
     */
    public static JbmLoginUser getLoginUser() {
        return LoginHelper.getLoginUser();
//        return SecurityContextHolder.get(JbmSecurityConstants.LOGIN_USER, JbmLoginUser.class);
    }

    /**
     * 获取请求token
     */
    public static String getToken() {
        return getToken(ServletUtils.getRequest());
    }

    /**
     * 根据request获取请求token
     */
    public static String getToken(HttpServletRequest request) {
        // 从header获取token标识
        String token = request.getHeader(JbmTokenConstants.AUTHENTICATION);
        return replaceTokenPrefix(token);
    }

    /**
     * 裁剪token前缀
     */
    public static String replaceTokenPrefix(String token) {
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (ObjectUtil.isNotEmpty(token) && token.startsWith(JbmTokenConstants.PREFIX)) {
            token = token.replaceFirst(JbmTokenConstants.PREFIX, "");
        }
        return token;
    }

    public static KeyPair generateRSAKey(String seed) {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048, seed.getBytes());
        return keyPair;
    }

    public static String generateRSAPublicKey(String seed) {
        String publicKey = Base64.encode(generateRSAKey(seed).getPublic().getEncoded());
        return publicKey;
    }

    public static String generateRSAPrivateKey(String seed) {
        String privateKey = Base64.encode(generateRSAKey(seed).getPrivate().getEncoded());
        return privateKey;
    }

    /**
     * 是否为管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword     真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
