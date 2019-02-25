package jbm.framework.cloud.node.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;


public class FeignAuth2RequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        SecurityContext securityContext= SecurityContextHolder.getContext();
        Authentication authentication=securityContext.getAuthentication();
        if(authentication!=null&&authentication.getDetails() instanceof OAuth2AuthenticationDetails ){
            OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails)authentication.getDetails();
            String token=String.format("%s %s","bearer",details.getTokenValue());
            requestTemplate.header("Authorization",token);

        }
    }
}
