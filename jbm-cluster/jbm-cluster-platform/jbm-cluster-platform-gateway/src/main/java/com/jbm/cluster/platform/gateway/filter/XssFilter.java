package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.jbm.cluster.platform.gateway.config.properties.XssProperties;
import com.jbm.cluster.platform.gateway.utils.PathMatcherUtils;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 跨站脚本过滤器
 *
 * @author wesley.zhang
 */
@Component
@ConditionalOnProperty(value = "security.xss.enabled", havingValue = "true")
public class XssFilter implements GlobalFilter, Ordered {
    // 跨站脚本的 xss 配置，nacos自行添加
    @Autowired
    private XssProperties xss;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // GET DELETE 不过滤
        HttpMethod method = request.getMethod();
        if (method == null || method.matches("GET") || method.matches("DELETE")) {
            return chain.filter(exchange);
        }
        // 非json类型，不过滤
        if (!isJsonRequest(exchange)) {
            return chain.filter(exchange);
        }
        // excludeUrls 不过滤
        String url = request.getURI().getPath();
        if (PathMatcherUtils.matches(url, xss.getExcludeUrls())) {
            return chain.filter(exchange);
        }
        ServerHttpRequestDecorator httpRequestDecorator = requestDecorator(exchange);
        return chain.filter(exchange.mutate().request(httpRequestDecorator).build());

    }

    private static final Map<String, Set<String>> WHITE_LIST = new HashMap<>();

    static {
        // ✅ 所有 Set 初始化均使用 JDK 8 支持的写法
        WHITE_LIST.put("p", new HashSet<>(Arrays.asList("class", "style")));
        WHITE_LIST.put("br", Collections.emptySet());
        WHITE_LIST.put("b", Collections.emptySet());
        WHITE_LIST.put("strong", Collections.emptySet());
        WHITE_LIST.put("i", Collections.emptySet());
        WHITE_LIST.put("em", Collections.emptySet());
        WHITE_LIST.put("u", Collections.emptySet());
        WHITE_LIST.put("s", Collections.emptySet());
        WHITE_LIST.put("sub", Collections.emptySet());
        WHITE_LIST.put("sup", Collections.emptySet());
        WHITE_LIST.put("ol", new HashSet<>(Arrays.asList("type", "start")));
        WHITE_LIST.put("ul", Collections.emptySet());
        WHITE_LIST.put("li", Collections.emptySet());
        WHITE_LIST.put("a", new HashSet<>(Arrays.asList("href", "title", "target")));
        WHITE_LIST.put("img", new HashSet<>(Arrays.asList("src", "alt", "title", "width", "height")));
        WHITE_LIST.put("blockquote", new HashSet<>(Arrays.asList("cite")));
        WHITE_LIST.put("pre", new HashSet<>(Arrays.asList("class")));
        WHITE_LIST.put("code", Collections.emptySet());
        WHITE_LIST.put("hr", Collections.emptySet());
    }


    public static String cleanHtmlTag(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }
        String filtered = HtmlUtil.filter(html);
        return cleanDangerousHref(filtered);
    }

    private static String cleanDangerousHref(String html) {
        // 替换危险协议 href（不区分大小写）
        return html.replaceAll("(?i)href\\s*=\\s*\"(javascript|data|vbscript):[^\">]*\"", "href=\"#\"")
                .replaceAll("(?i)href\\s*=\\s*'((javascript|data|vbscript):[^'>]*)'", "href='#'");
    }


    private ServerHttpRequestDecorator requestDecorator(ServerWebExchange exchange) {
        ServerHttpRequestDecorator serverHttpRequestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                Flux<DataBuffer> body = super.getBody();
                return body.buffer().map(dataBuffers -> {
                    DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                    DataBuffer join = dataBufferFactory.join(dataBuffers);
                    byte[] content = new byte[join.readableByteCount()];
                    join.read(content);
                    DataBufferUtils.release(join);
                    String bodyStr = new String(content, StandardCharsets.UTF_8);
                    // 防xss攻击过滤
                    bodyStr = cleanHtmlTag(bodyStr);
                    // 转成字节
                    byte[] bytes = bodyStr.getBytes();
                    NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
                    DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
                    buffer.write(bytes);
                    return buffer;
                });
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                // 由于修改了请求体的body，导致content-length长度不确定，因此需要删除原先的content-length
                httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                return httpHeaders;
            }

        };
        return serverHttpRequestDecorator;
    }

    /**
     * 是否是Json请求
     *
     * @param exchange HTTP请求
     */
    public boolean isJsonRequest(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return StrUtil.startWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
