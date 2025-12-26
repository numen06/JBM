package com.jbm.cluster.common.satoken.oauth;


import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.common.satoken.config.TokenConfig;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * jbm默认自己节点的oauth认证
 *
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
public class JbmNodeOAuth2TemplateImpl extends SaOAuth2Template implements InitializingBean {

    private TokenConfig tokenConfig;
    
    private LoadingCache<String, ClientTokenModel> clientTokenModelLoadingCache;

    public JbmNodeOAuth2TemplateImpl() {
        // 延迟初始化，避免循环依赖
        this.clientTokenModelLoadingCache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS) // 默认24小时
                .build(key -> super.generateClientToken(key + "-" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN), "*")
                );
    }


    @Override
    public ClientTokenModel generateClientToken(String clientId, String scope) {
        try {
            return clientTokenModelLoadingCache.get(clientId);
        } catch (Exception e) {
            log.error("取应用程序TOKEN失败", e);
            throw e;
        }
    }

    @Override
    public SaClientModel getClientModel(String clientId) {
        try {
            Map<String, ClientModelSource> clientModelSourceMap = SpringUtil.getBeansOfType(ClientModelSource.class);
            for (Map.Entry<String, ClientModelSource> entry : clientModelSourceMap.entrySet()) {
                ClientModelSource v = entry.getValue();
                SaClientModel saClientModel = v.getClientModel(clientId);
                if (ObjectUtil.isNotEmpty(saClientModel)) {
                    return saClientModel;
                }
            }
        } catch (Exception e) {
            log.error("取应用程序TOKEN失败", e);
        }
        return null;
//        ClientTokenModel clientTokenModel = getClientToken(getClientTokenValue(clientId));
//        if (ObjectUtil.isNotEmpty(clientTokenModel)) {
//            return new SaClientModel()
//                    .setClientId(clientTokenModel.clientId)
//                    .setClientSecret(UUID.randomUUID().toString())
//                    .setAllowUrl("*")
//                    .setContractScope("*")
//                    .setIsAutoMode(true);
//        }
    }

    @Override
    public AccessTokenModel checkAccessToken(String accessToken) {
        return super.checkAccessToken(accessToken);
    }

    // 根据ClientId 和 LoginId 获取openid
    @Override
    public String getOpenid(String clientId, Object loginId) {
        // 此为模拟数据，真实环境需要从数据库查询
        log.info("登录过程中，获取OPENID");
        if (ObjectUtil.isEmpty(LoginHelper.getLoginUser(loginId))) {
            return StrUtil.toString(loginId);
        }
        return LoginHelper.getLoginUser(loginId).getOpenId();
    }

    @Override
    public void checkScope(String accessToken, String... scopes) {
        super.checkScope(accessToken, scopes);
    }

    /**
     * 生成accessToken
     */
    @Override
    public String randomAccessToken(String clientId, Object loginId, String scope) {
        String token = null;
        try {
            String tmp = StpUtil.getTokenValueByLoginId(loginId);
            if (StrUtil.isNotEmpty(tmp)) {
                token = tmp;
            } else {
                //没有的话，创建一个
                token = StpUtil.createLoginSession(loginId);
            }
        } catch (Exception e) {
            log.error("获取用户token失败", e);
        }
        return token;
    }
    // -------------- 其它需要重写的函数


    @Override
    public AccessTokenModel refreshAccessToken(String refreshToken) {
        // 先调用父类方法获取新的 AccessTokenModel（这会验证 refreshToken 并更新 OAuth2 存储）
        AccessTokenModel accessTokenModel = super.refreshAccessToken(refreshToken);
        
        if (accessTokenModel == null) {
            log.error("刷新Token失败：父类返回null");
            throw new RuntimeException("无效的refreshToken");
        }
        
        // 从新的 AccessTokenModel 中获取 loginId
        Object loginId = accessTokenModel.loginId;
        String oldAccessToken = accessTokenModel.accessToken;
        
        log.info("刷新Token：loginId={}, 旧accessToken={}", loginId, oldAccessToken);
        
        // 获取用户信息，用于重新登录
        com.jbm.cluster.api.model.auth.JbmLoginUser loginUser = LoginHelper.getLoginUser(loginId);
        if (loginUser == null) {
            log.error("刷新Token失败：无法获取用户信息，loginId={}", loginId);
            throw new RuntimeException("用户信息不存在");
        }
        
        // 踢掉旧 token（仅针对当前 access_token，不影响同一用户其它端）
        try {
            StpUtil.logoutByTokenValue(oldAccessToken);
            log.info("已踢掉旧token，loginId={}, accessToken={}", loginId, oldAccessToken);
        } catch (Exception e) {
            log.warn("踢掉旧token时出现异常（可能已经失效）: {}", e.getMessage());
        }
        
        // 重新登录生成新 token
        LoginHelper.login(loginUser);
        String newToken = StpUtil.getTokenValue();
        log.info("已生成新token，loginId={}, 新accessToken={}", loginId, newToken);
        
        // 确保新的 accessToken 就是 Sa-Token 的 token
        accessTokenModel.accessToken = newToken;
        
        log.info("刷新Token成功：loginId={}, 新accessToken={}, 过期时间={}", 
                loginId, newToken, StpUtil.getTokenInfo().getTokenActivityTimeout());
        
        return accessTokenModel;
    }

    @Override
    public String randomClientToken(String clientId, String scope) {
        //使用IdToken接管
        final String idToken = SaIdUtil.getToken();
//        SaIdUtil.saIdTemplate.saveToken(idToken);
        return idToken;
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 获取TokenConfig并重新初始化cache
        try {
            this.tokenConfig = SpringUtil.getBean(TokenConfig.class);
            // 重新创建cache，使用配置的时间
            this.clientTokenModelLoadingCache = Caffeine.newBuilder()
                    .expireAfterWrite(tokenConfig.getClientTokenCacheHours(), TimeUnit.HOURS)
                    .build(key -> super.generateClientToken(key + "-" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN), "*")
                    );
            log.info("初始化当前应用的默认的ClientToken，缓存时间: {}小时", tokenConfig.getClientTokenCacheHours());
        } catch (Exception e) {
            log.warn("无法获取TokenConfig，使用默认24小时缓存时间");
        }
        this.generateClientToken(SpringUtil.getApplicationName(), "*");
    }
}