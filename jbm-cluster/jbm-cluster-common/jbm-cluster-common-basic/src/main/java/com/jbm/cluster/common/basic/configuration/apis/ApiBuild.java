package com.jbm.cluster.common.basic.configuration.apis;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.cluster.common.basic.annotation.AccessLogIgnore;
import com.jbm.util.StringUtils;
import org.springframework.util.AntPathMatcher;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/4/30 19:24
 * @Description TODO
 */
@Data
public class ApiBuild {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RequestMappingInfo requestMappingInfo;
    private final HandlerMethod handlerMethod;
    private final String serviceId;
    private final String[] permitAllPatterns;

    public ApiBuild(RequestMappingInfo requestMappingInfo, HandlerMethod handlerMethod, String serviceId) {
        this(requestMappingInfo, handlerMethod, serviceId, new String[0]);
    }

    public ApiBuild(RequestMappingInfo requestMappingInfo, HandlerMethod handlerMethod, String serviceId, String[] permitAllPatterns) {
        this.requestMappingInfo = requestMappingInfo;
        this.handlerMethod = handlerMethod;
        this.serviceId = serviceId;
        this.permitAllPatterns = permitAllPatterns != null ? permitAllPatterns : new String[0];
    }


    public JbmApi doBuild() {
        if (handlerMethod.getMethodAnnotation(ApiIgnore.class) != null) {
            // 忽略的接口不扫描
            return null;
        }
        Set<MediaType> mediaTypeSet = requestMappingInfo.getProducesCondition().getProducibleMediaTypes();
        for (MethodParameter params : handlerMethod.getMethodParameters()) {
            if (params.hasParameterAnnotation(RequestBody.class)) {
                mediaTypeSet.add(MediaType.APPLICATION_JSON);
                break;
            }
        }
        // 请求类型
        RequestMethodsRequestCondition methodsCondition = requestMappingInfo.getMethodsCondition();
        // 请求路径
        String url = StrUtil.join(StrUtil.COMMA, requestMappingInfo.getPatternsCondition().getPatterns());
        JbmApi.JbmApiBuilder api = JbmApi.builder();
        // 类名
        String className = handlerMethod.getMethod().getDeclaringClass().getName();
        // 方法名
        String methodName = handlerMethod.getMethod().getName();
        //主动忽略日志,当有忽略注解的接口,则不记录日志
        Boolean accessLog = !handlerMethod.hasMethodAnnotation(AccessLogIgnore.class);
        String md5 = DigestUtil.md5Hex(serviceId + url);
        String name = StrUtil.EMPTY;
        String desc = StrUtil.EMPTY;
        // 是否需要安全认证：@PermitAll 与 jbm.cluster.permit-all 白名单为 false
        Boolean isAuth = !isPermitAll();
        ApiOperation apiOperation = handlerMethod.getMethodAnnotation(ApiOperation.class);
        if (ObjectUtil.isNotEmpty(apiOperation)) {
            name = apiOperation.value();
            desc = apiOperation.notes();
        }
        name = StringUtils.isBlank(name) ? methodName : name;
        api.apiName(name)
                .methodName(methodName)
                .accessLog(accessLog)
                .apiCode(md5)
                .apiDesc(desc)
                .paths(requestMappingInfo.getPatternsCondition().getPatterns())
                .className(className)
                .md5(md5)
                .requestMethods(methodsCondition.getMethods().stream().map(RequestMethod::toString).collect(Collectors.toSet()))
                .serviceId(serviceId)
                .contentTypes(requestMappingInfo.getProducesCondition().getProducibleMediaTypes().stream().map(MediaType::toString).collect(Collectors.toSet()))
                .isAuth(isAuth);
        return api.build();
    }

    private static final String PERMIT_ALL_ANNOTATION = "com.jbm.cluster.common.security.annotation.PermitAll";

    private boolean isPermitAll() {
        if (handlerMethod.getMethod().getAnnotations().length > 0) {
            for (java.lang.annotation.Annotation annotation : handlerMethod.getMethod().getAnnotations()) {
                if (PERMIT_ALL_ANNOTATION.equals(annotation.annotationType().getName())) {
                    return true;
                }
            }
        }
        Set<String> patterns = requestMappingInfo.getPatternsCondition().getPatterns();
        for (String path : patterns) {
            for (String permitPattern : permitAllPatterns) {
                if (PATH_MATCHER.match(permitPattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }


    private String getMediaTypes(Set<MediaType> mediaTypes) {
        StringBuilder sbf = new StringBuilder();
        for (MediaType mediaType : mediaTypes) {
            sbf.append(mediaType.toString()).append(StrUtil.COMMA);
        }
        if (!mediaTypes.isEmpty()) {
            sbf.deleteCharAt(sbf.length() - 1);
        }
        return sbf.toString();
    }

    private String getMethods(Set<RequestMethod> requestMethods) {
        StringBuilder sbf = new StringBuilder();
        for (RequestMethod requestMethod : requestMethods) {
            sbf.append(requestMethod.toString()).append(StrUtil.COMMA);
        }
        if (!requestMethods.isEmpty()) {
            sbf.deleteCharAt(sbf.length() - 1);
        }
        return sbf.toString();
    }


}
