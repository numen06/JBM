package com.jbm.framework.cloud.auth.config;

import com.jbm.framework.cloud.auth.constant.JbmCommonConstant;
import com.jbm.framework.cloud.auth.constant.JbmSecurityConstants;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import com.jbm.framework.cloud.auth.token.JbmRedisTokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public TokenStore redisTokenStore() {
        JbmRedisTokenStore tokenStore = new JbmRedisTokenStore(redisTemplate);
        return tokenStore;
//        return new InMemoryTokenStore();
    }


//    @Bean
//    public JwtAccessTokenConverter jwtAccessTokenConverter() {
//        JbmJwtAccessTokenConverter jwtAccessTokenConverter = new JbmJwtAccessTokenConverter();
//        jwtAccessTokenConverter.setSigningKey(JbmCommonConstant.SIGN_KEY);
//        return jwtAccessTokenConverter;
//    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
//		security.passwordEncoder(passwordEncoder);
        security.tokenKeyAccess("permitAll()");
        security.checkTokenAccess("isAuthenticated()");
        security.allowFormAuthenticationForClients();
    }


    @Primary
    @Bean
    public AuthorizationServerTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setAccessTokenValiditySeconds(-1);
        defaultTokenServices.setRefreshTokenValiditySeconds(-1);
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setReuseRefreshToken(false);
        defaultTokenServices.setTokenStore(redisTokenStore());
        return defaultTokenServices;
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        //普通token
//        endpoints.authenticationManager(authenticationManager).tokenStore(redisTokenStore());
        //设置永不过期token
        endpoints.authenticationManager(authenticationManager).tokenServices(tokenServices());
//        //jwt token增强配置
//        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
//        tokenEnhancerChain.setTokenEnhancers(
//                Arrays.asList(tokenEnhancer(), jwtAccessTokenConverter()));
//
//        endpoints
//                .tokenStore(redisTokenStore())
//                .tokenEnhancer(tokenEnhancerChain)
//                .authenticationManager(authenticationManager)
//                .reuseRefreshTokens(false)
//                .userDetailsService(userDetailsService);
    }

//    @Bean
//    public TokenEnhancer tokenEnhancer() {
//        return (accessToken, authentication) -> {
//            final Map<String, Object> additionalInfo = new HashMap<>(2);
//            additionalInfo.put("license", JbmSecurityConstants.LICENSE);
//            JbmAuthUser user = (JbmAuthUser) authentication.getUserAuthentication().getPrincipal();
//            if (user != null) {
//                additionalInfo.put("userId", user.getUserId());
//            }
//            ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
//            return accessToken;
//        };
//    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 配置两个客户端,一个用于password认证一个用于client认证
        clients.inMemory().withClient("client")
                .authorizedGrantTypes("client_credentials", "refresh_token").scopes("select").authorities("client")
                .secret(passwordEncoder().encode("123456")).and().withClient("platform_server")
                .authorizedGrantTypes("password", "refresh_token").scopes("select").authorities("client")
                .secret(passwordEncoder().encode("123456")).and().withClient("client_2")
                .authorizedGrantTypes("password", "refresh_token").scopes("select").authorities("client")
                .secret(passwordEncoder().encode("123456"));
    }

//	@Primary
//	@Bean
//	public DefaultTokenServices defaultTokenServices(ClientDetailsServiceConfigurer clients) throws Exception {
//		DefaultTokenServices tokenServices = new DefaultTokenServices();
//		tokenServices.setTokenStore(tokenStore());
//		tokenServices.setSupportRefreshToken(true);
//		tokenServices.setClientDetailsService(clientDetails(clients));
//		tokenServices.setAccessTokenValiditySeconds(60 * 60 * 12); // token有效期自定义设置，默认12小时
//		tokenServices.setRefreshTokenValiditySeconds(60 * 60 * 24 * 7);// 默认30天，这里修改
//		return tokenServices;
//	}

//	@Bean
//	public ClientDetailsService clientDetails(ClientDetailsServiceConfigurer clients) throws Exception {
////		return new JdbcClientDetailsService(dataSource);
//		ClientDetailsService clientDetailsService = new InMemoryClientDetailsService();
//		clientDetailsService = clients.and().build();
//
////		Map<String, ? extends ClientDetails> clientDetailsStore = new HashMap<String, ClientDetails>();
////		clientDetailsStore.put("android", new BaseClientDetails());
////		clientDetailsService.setClientDetailsStore(clientDetailsStore);
//		return clientDetailsService;
//	}
//
//	@Configuration
//	@Order(-20)
//	protected static class AuthenticationManagerConfiguration extends GlobalAuthenticationConfigurerAdapter {
//
//		@Autowired
//		private DataSource dataSource;
//
//		@Override
//		public void init(AuthenticationManagerBuilder auth) throws Exception {
////			auth.jdbcAuthentication().dataSource(dataSource).withUser("dave").password("secret").roles("USER").and().withUser("anil").password("password").roles("ADMIN");
//		}
//	}

}