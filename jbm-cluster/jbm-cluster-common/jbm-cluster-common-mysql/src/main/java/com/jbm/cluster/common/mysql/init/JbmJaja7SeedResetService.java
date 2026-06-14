package com.jbm.cluster.common.mysql.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.common.mysql.mapper.BaseAccountMapper;
import com.jbm.cluster.common.mysql.mapper.BaseAppMapper;
import com.jbm.cluster.common.mysql.mapper.BaseUserMapper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * jaja7 本地：恢复 admin 默认密码、JBM 种子应用 OAuth 凭证、清除登录锁定。
 */
@Slf4j
@Service
public class JbmJaja7SeedResetService {

    private static final long JBM_SEED_APP_ID = 1000L;

    @Value("${jbm.cluster.data-init.root-password:admin}")
    private String rootPassword;

    @Autowired
    private BaseUserMapper baseUserMapper;
    @Autowired
    private BaseAccountMapper baseAccountMapper;
    @Autowired
    private BaseAppMapper baseAppMapper;
    @Autowired(required = false)
    private RedisService redisService;

    public Map<String, Object> resetAll() {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!existsRootUser()) {
            out.put("skipped", true);
            out.put("reason", "no admin user");
            return out;
        }
        int accounts = resetRootAccounts();
        boolean appOk = resetJbmSeedAppCredentials();
        clearLoginErrorCounter();
        out.put("adminPassword", rootPassword);
        out.put("accountsUpdated", accounts);
        out.put("jbmAppCredentialsReset", appOk);
        out.put("clientId", JbmConstants.JBM_APP_API_KEY);
        out.put("clientSecretPlain", JbmConstants.JBM_APP_SECRET);
        out.put("loginLockCleared", true);
        return out;
    }

    private boolean existsRootUser() {
        QueryWrapper<BaseUser> q = new QueryWrapper<>();
        q.lambda().eq(BaseUser::getUserName, JbmConstants.ROOT_USER_NAME);
        return baseUserMapper.selectCount(q) > 0;
    }

    private int resetRootAccounts() {
        QueryWrapper<BaseAccount> q = new QueryWrapper<>();
        q.lambda().eq(BaseAccount::getUserId, JbmConstants.ROOT_USER_ID)
                .or()
                .eq(BaseAccount::getAccount, JbmConstants.ROOT_USER_NAME);
        List<BaseAccount> accounts = baseAccountMapper.selectList(q);
        Date now = new Date();
        String encoded = SecurityUtils.encryptPassword(rootPassword);
        for (BaseAccount account : accounts) {
            BaseAccount update = new BaseAccount();
            update.setAccountId(account.getAccountId());
            update.setPassword(encoded);
            update.setMustChangePassword(JbmConstants.ENABLED);
            update.setUpdateTime(now);
            baseAccountMapper.updateById(update);
        }
        return accounts.size();
    }

    public boolean resetJbmSeedAppCredentials() {
        BaseApp app = baseAppMapper.selectById(JBM_SEED_APP_ID);
        if (app == null) {
            QueryWrapper<BaseApp> q = new QueryWrapper<>();
            q.lambda().eq(BaseApp::getApiKey, JbmConstants.JBM_APP_API_KEY);
            app = baseAppMapper.selectOne(q);
        }
        if (app == null) {
            log.warn("JBM seed app not found appId={} apiKey={}", JBM_SEED_APP_ID, JbmConstants.JBM_APP_API_KEY);
            return false;
        }
        BaseApp update = new BaseApp();
        update.setAppId(app.getAppId());
        update.setApiKey(JbmConstants.JBM_APP_API_KEY);
        update.setSecretKey(SecurityUtils.encryptPassword(JbmConstants.JBM_APP_SECRET));
        update.setUpdateTime(new Date());
        baseAppMapper.updateById(update);
        log.info("JBM seed app credentials restored appId={} apiKey={}", app.getAppId(), JbmConstants.JBM_APP_API_KEY);
        return true;
    }

    private void clearLoginErrorCounter() {
        if (redisService == null) {
            return;
        }
        try {
            redisService.deleteObject(JbmCacheConstants.LOGIN_ERROR + JbmConstants.ROOT_USER_NAME);
        } catch (Exception e) {
            log.warn("clear login error counter failed: {}", e.getMessage());
        }
    }
}
