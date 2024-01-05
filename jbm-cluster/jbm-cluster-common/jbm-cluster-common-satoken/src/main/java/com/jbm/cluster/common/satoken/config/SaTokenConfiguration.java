package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import com.jbm.cluster.common.satoken.core.dao.RedisSaTokenDao;
import com.jbm.cluster.common.satoken.core.service.SaPermissionImpl;
import com.jbm.cluster.common.satoken.oauth.JbmNodeOAuth2TemplateImpl;
import com.jbm.cluster.common.satoken.oauth.NodeClientModelSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 功能点	Simple 简单模式	Mixin 混入模式	Stateless 无状态模式
 * Token风格	jwt风格	jwt风格	jwt风格
 * 登录数据存储	Redis中	Token中	Token中
 * Session存储	Redis中	Redis中	无Session
 * 注销下线	前后端双清数据	前后端双清数据	前端清除数据
 * 踢人下线API	支持	不支持	不支持
 * 登录认证	支持	支持	支持
 * 角色认证	支持	支持	支持
 * 权限认证	支持	支持	支持
 * timeout 有效期	支持	支持	支持
 * activity-timeout 有效期	支持	支持	不支持
 * id反查Token	支持	支持	不支持
 * 会话管理	支持	部分支持	不支持
 * 注解鉴权	支持	支持	支持
 * 路由拦截鉴权	支持	支持	支持
 * 账号封禁	支持	支持	不支持
 * 身份切换	支持	支持	支持
 * 二级认证	支持	支持	支持
 * 模式总结	Token风格替换	jwt 与 Redis 逻辑混合	完全舍弃Redis，只用jwt
 * Sa-Token 配置
 *
 * @author wesley
 */
@Configuration
public class SaTokenConfiguration {

    // Sa-Token 整合 jwt (Simple 简单模式)
    @Bean
    public StpLogicJwtForSimple getStpLogicJwt() {
//        UserLogicJwt userLogicJwt = new UserLogicJwt();
//        SaManager.putStpLogic(userLogicJwt);
//        SaManager.putStpLogic(new AdminLogicJwt());
//        return userLogicJwt;
        StpLogicJwtForSimple stpLogicJwtForSimple = new StpLogicJwtForSimple();
        return stpLogicJwtForSimple;
    }

//    @Bean
//    public UserLogicJwt getUserLogicJwt() {
//        return new UserLogicJwt();
//    }
//
//    @Bean
//    public AdminLogicJwt getAdminLogicJwt() {
//        return new AdminLogicJwt();
//    }

    @Bean
    public RedisSaTokenDao redisSaTokenDao(SaTokenConfig saTokenConfig) {
        SaManager.setConfig(saTokenConfig);
        return new RedisSaTokenDao();
    }

    @Bean
    public SaPermissionImpl saPermissionImpl() {
        return new SaPermissionImpl();
    }


    @Bean
    @Primary
    public SaOAuth2Template jbmNodeOAuth2Template(RedisSaTokenDao redisSaTokenDao) {
        SaManager.setSaTokenDao(redisSaTokenDao);
        return new JbmNodeOAuth2TemplateImpl();
    }

    @Bean
    public NodeClientModelSource nodeClientModelSource() {
        return new NodeClientModelSource();
    }

}
