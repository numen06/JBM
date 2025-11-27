package jbm.framework.boot.autoconfigure.swagger.config;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Swagger基础认证过滤器
 *
 * @author wesley.zhang
 */
public class SwaggerBasicAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String WWW_AUTHENTICATE_VALUE = "Basic realm=\"Swagger API\"";

    private final SwaggerProperties.BasicAuth basicAuth;

    public SwaggerBasicAuthFilter(SwaggerProperties.BasicAuth basicAuth) {
        this.basicAuth = basicAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!basicAuth.getEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BASIC_PREFIX)) {
            String base64Credentials = authHeader.substring(BASIC_PREFIX.length());
            try {
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];
                    if (basicAuth.getUsername().equals(username) && basicAuth.getPassword().equals(password)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
            } catch (IllegalArgumentException e) {
                // Base64解码失败，继续执行返回401
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
        response.getWriter().write("Unauthorized");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 只对Swagger相关路径进行过滤
        return !path.startsWith("/swagger-ui") && 
               !path.startsWith("/v2/api-docs") && 
               !path.startsWith("/swagger-resources") &&
               !path.equals("/swagger-ui.html");
    }
}

