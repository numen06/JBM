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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * jbm默认自己节点的oauth认证
 *
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
public class JbmNodeOAuth2TemplateImpl extends SaOAuth2Template implements InitializingBean, ApplicationContextAware, ApplicationListener<ApplicationReadyEvent> {

    private TokenConfig tokenConfig;
    
    private LoadingCache<String, ClientTokenModel> clientTokenModelLoadingCache;
    
    private ApplicationContext applicationContext;
    
    private volatile boolean clientTokenInitialized = false;

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
     * 初始化方法，只负责配置缓存，不进行耗时的token生成操作
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        long startTime = System.currentTimeMillis();
        log.debug("开始初始化 JbmNodeOAuth2TemplateImpl");
        
        // 优化TokenConfig获取方式，使用ApplicationContext安全获取
        try {
            if (applicationContext != null) {
                // 尝试从ApplicationContext获取TokenConfig，如果不存在则返回null
                try {
                    this.tokenConfig = applicationContext.getBean(TokenConfig.class);
                    log.debug("成功获取TokenConfig，缓存时间: {}小时", tokenConfig.getClientTokenCacheHours());
                } catch (Exception e) {
                    log.debug("ApplicationContext中未找到TokenConfig，尝试使用SpringUtil获取: {}", e.getMessage());
                    // 降级方案：使用SpringUtil获取
                    this.tokenConfig = SpringUtil.getBean(TokenConfig.class);
                }
            } else {
                // 如果ApplicationContext还未注入，尝试使用SpringUtil
                this.tokenConfig = SpringUtil.getBean(TokenConfig.class);
            }
            
            // 重新创建cache，使用配置的时间
            if (this.tokenConfig != null) {
                this.clientTokenModelLoadingCache = Caffeine.newBuilder()
                        .expireAfterWrite(tokenConfig.getClientTokenCacheHours(), TimeUnit.HOURS)
                        .build(key -> super.generateClientToken(key + "-" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN), "*")
                        );
                log.info("初始化ClientToken缓存完成，缓存时间: {}小时", tokenConfig.getClientTokenCacheHours());
            } else {
                log.warn("无法获取TokenConfig，使用默认24小时缓存时间");
            }
        } catch (Exception e) {
            log.warn("无法获取TokenConfig，使用默认24小时缓存时间，异常: {}", e.getMessage());
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.debug("JbmNodeOAuth2TemplateImpl初始化完成，耗时: {}ms", duration);
        
        // 不再同步调用generateClientToken，改为在ApplicationReadyEvent中异步执行
    }
    
    /**
     * 设置ApplicationContext，用于安全获取Bean
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 监听应用启动完成事件，异步初始化ClientToken
     * 避免阻塞主线程启动流程
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (clientTokenInitialized) {
            return;
        }
        
        log.info("应用启动完成，开始异步初始化ClientToken");
        long startTime = System.currentTimeMillis();
        
        // 异步初始化ClientToken，避免阻塞
        CompletableFuture.runAsync(() -> {
            try {
                String applicationName = SpringUtil.getApplicationName();
                log.debug("开始生成ClientToken，应用名称: {}", applicationName);
                
                // 延迟一小段时间，确保所有Bean都已完全初始化
                Thread.sleep(100);
                
                this.generateClientToken(applicationName, "*");
                clientTokenInitialized = true;
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("ClientToken异步初始化完成，应用名称: {}，耗时: {}ms", applicationName, duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("ClientToken异步初始化失败，耗时: {}ms，错误: {}", duration, e.getMessage(), e);
                // 不抛出异常，允许应用继续运行
            }
        });
    }
}