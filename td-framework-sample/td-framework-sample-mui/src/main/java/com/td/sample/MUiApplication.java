package com.td.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@SpringBootApplication
@EnableOAuth2Sso
@EnableZuulProxy
@ComponentScan("com.td")
public class MUiApplication extends WebSecurityConfigurerAdapter {

	public static void main(String[] args) {
		SpringApplication.run(MUiApplication.class, args);
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/assets/**", "/app/**", "/lib/**");
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.logout().and().authorizeRequests().antMatchers("/login").permitAll().anyRequest().authenticated().and().csrf()
			.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
	}
}