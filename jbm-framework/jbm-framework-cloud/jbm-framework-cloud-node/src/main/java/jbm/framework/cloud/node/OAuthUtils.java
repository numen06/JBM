package jbm.framework.cloud.node;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import com.jbm.framework.exceptions.ServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 * oauth工具类
 */
public class OAuthUtils {


    /**
     * 获取当前请求用户
     *
     * @return 当前用户
     */
    public static JbmAuthUser getCurrentJbmAuthUser() {
        OAuth2Authentication principal = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        return getCurrentJbmAuthUser(principal);
    }

    /**
     * 通过token获取用户
     *
     * @param authentication token
     * @return 用户
     */
    public static JbmAuthUser getCurrentJbmAuthUser(OAuth2Authentication authentication) {
        JbmAuthUser jbmAuthUser = JSON.parseObject(JSON.toJSONString(authentication.getUserAuthentication().getDetails()), JbmAuthUser.class);
        if (jbmAuthUser.getUserId() == null) {
            throw new ServiceException("没有获取到当前用户信息");
        }
        return jbmAuthUser;
    }

    /**
     * 获取当前用户token信息
     *
     * @return
     */
    public static String getCurrenToken() {
        try {
            OAuth2AuthenticationDetails details = getCurrenAuthenticationDetails();
            if (ObjectUtil.isEmpty(details))
                return null;
            return details.getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 获取当前用户token信息
     *
     * @return
     */
    public static OAuth2AuthenticationDetails getCurrenAuthenticationDetails() {
        OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        return getCurrenAuthenticationDetails(authentication);
    }

    /**
     * 获取用户的token
     *
     * @param authentication
     * @return
     */
    public static OAuth2AuthenticationDetails getCurrenAuthenticationDetails(OAuth2Authentication authentication) {
        if (authentication.getDetails() instanceof OAuth2AuthenticationDetails) {
            return ((OAuth2AuthenticationDetails) authentication.getDetails());
        }
        return null;
    }
}
