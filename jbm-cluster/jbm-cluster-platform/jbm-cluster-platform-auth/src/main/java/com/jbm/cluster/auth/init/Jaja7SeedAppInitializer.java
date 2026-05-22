package com.jbm.cluster.auth.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.mysql.mapper.BaseAppMapper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * jaja7 环境下，为 OAuth2 测试注入默认应用（base_app）。
 * 仅在 apiKey 不存在时插入，避免影响已有数据。
 */
@Slf4j
@Component
@Profile("jaja7")
public class Jaja7SeedAppInitializer implements ApplicationRunner {

    // 保持与 SysLoginService.decryptPassword 的 demo 兼容逻辑一致（demo 可用明文密码）
    public static final String SEED_API_KEY = "demo";
    public static final String SEED_SECRET_PLAIN = "demo123";

    private final BaseAppMapper baseAppMapper;
    private final RedisService redisService;

    public Jaja7SeedAppInitializer(BaseAppMapper baseAppMapper, RedisService redisService) {
        this.baseAppMapper = baseAppMapper;
        this.redisService = redisService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 避免历史失败次数导致测试直接被锁定
        try {
            redisService.deleteObject(JbmCacheConstants.LOGIN_ERROR + "admin");
        } catch (Exception e) {
            log.warn("Reset login error counter failed: {}", e.getMessage());
        }

        QueryWrapper<BaseApp> q = new QueryWrapper<>();
        q.lambda().eq(BaseApp::getApiKey, SEED_API_KEY);
        BaseApp exist = baseAppMapper.selectOne(q);
        if (exist != null) {
            log.info("Seed app already exists: apiKey={}", SEED_API_KEY);
            return;
        }

        Date now = new Date();
        BaseApp app = new BaseApp();
        app.setAppId(1001L);
        app.setId(1001L);
        app.setApiKey(SEED_API_KEY);
        app.setSecretKey(SecurityUtils.encryptPassword(SEED_SECRET_PLAIN));
        app.setAppName("DEMO OAuth2 测试应用");
        app.setAppNameEn("demo-oauth2-test");
        app.setAppType("pc");
        // 部分库表 schema 约束 app_icon 非空
        app.setAppIcon("");
        app.setAppIcons("");
        app.setDeveloperId(0L);
        app.setWebsite("*");
        app.setStatus(1);
        app.setIsPersist(0);
        app.setCreateTime(now);
        app.setUpdateTime(now);

        baseAppMapper.insert(app);
        log.info("Seed app inserted: apiKey={}, secretPlain={}", SEED_API_KEY, SEED_SECRET_PLAIN);
    }
}

