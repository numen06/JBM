package jbm.framework.cloud.node;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

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
     * @param principal token
     * @return 用户
     */
    public static JbmAuthUser getCurrentJbmAuthUser(OAuth2Authentication principal) {
        JbmAuthUser jbmAuthUser = JSON.parseObject(JSON.toJSONString(principal.getUserAuthentication().getDetails()), JbmAuthUser.class);
        return jbmAuthUser;
    }


}
