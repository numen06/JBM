package jbm.framework.boot.autoconfigure.fastjson;

import com.jbm.util.sensitive.SensitiveContext;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求结束时清理脱敏上下文，确保 skipMask 覆盖 JSON 序列化阶段。
 */
public class SensitiveContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            SensitiveContext.clear();
        }
    }
}
