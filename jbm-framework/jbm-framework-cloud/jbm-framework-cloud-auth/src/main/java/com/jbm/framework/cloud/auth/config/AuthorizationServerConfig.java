package com.jbm.framework.cloud.auth.config;

import com.jbm.framework.cloud.auth.constant.JbmSecurityConstants;
import com.jbm.framework.cloud.auth.feign.UserService;
import com.jbm.framework.cloud.auth.token.JbmRedisTokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate redisTemplate;

    @Bean
    public TokenStore tokenStore() {
//		return new JdbcTokenStore(dataSource);
//        return new InMemoryTokenStore();
        if (redisTemplate == null) {
            return new InMemoryTokenStore();
        }
        JbmRedisTokenStore tokenStore = new JbmRedisTokenStore(redisTemplate);
        return tokenStore;
    }


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

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(authenticationManager).tokenStore(tokenStore());
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
//		clients.jdbc(dataSource).passwordEncoder(passwordEncoder).withClient("client").secret("secret")
//				.authorizedGrantTypes("password", "refresh_token").scopes("read", "write")
//				.accessTokenValiditySeconds(3600) // 1 hour
//				.refreshTokenValiditySeconds(2592000) // 30 days
//				.and().withClient("svca-service").secret("password")
//				.authorizedGrantTypes("client_credentials", "refresh_token").scopes("server").and()
//				.withClient("svcb-service").secret("password")
//				.authorizedGrantTypes("client_credentials", "refresh_token").scopes("server");
//		clients.withClientDetails(clientDetails());
//		clients.inMemory().withClient("android").scopes("xx") // 此处的scopes是无用的，可以随意设置
//				.secret(passwordEncoder().encode("android"))
//				.authorizedGrantTypes("password", "authorization_code", "refresh_token").and().withClient("webapp")
//				.scopes("xx").authorizedGrantTypes("implicit");
//}
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