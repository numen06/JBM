package com.jbm.cluster.common.basic.configuration.apis;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.util.StringUtils;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
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
 * @Created wesley.zhang
 * @Date 2022/4/30 19:24
 * @Description TODO
 */
@Data
@AllArgsConstructor
public class ApiBuild {

    private RequestMappingInfo requestMappingInfo;
    private HandlerMethod handlerMethod;
    private String serviceId;


    public JbmApi doBuild() {
        if (handlerMethod.getMethodAnnotation(ApiIgnore.class) != null) {
            // 忽略的接口不扫描
            return null;
        }
        Set<MediaType> mediaTypeSet = requestMappingInfo.getProducesCondition().getProducibleMediaTypes();
        for (MethodParameter params : handlerMethod.getMethodParameters()) {
            if (params.hasParameterAnnotation(RequestBody.class)) {
                mediaTypeSet.add(MediaType.APPLICATION_JSON_UTF8);
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
        String md5 = DigestUtil.md5Hex(serviceId + url);
        String name = StrUtil.EMPTY;
        String desc = StrUtil.EMPTY;
        // 是否需要安全认证 默认:1-是 0-否
        Boolean isAuth = true;
        // 匹配项目中.permitAll()配置
        ApiOperation apiOperation = handlerMethod.getMethodAnnotation(ApiOperation.class);
        if (ObjectUtil.isNotEmpty(apiOperation)) {
            name = apiOperation.value();
            desc = apiOperation.notes();
        }
        name = StringUtils.isBlank(name) ? methodName : name;
        api.apiName(name)
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


    private String getMediaTypes(Set<MediaType> mediaTypes) {
        StringBuilder sbf = new StringBuilder();
        for (MediaType mediaType : mediaTypes) {
            sbf.append(mediaType.toString()).append(StrUtil.COMMA);
        }
        if (mediaTypes.size() > 0) {
            sbf.deleteCharAt(sbf.length() - 1);
        }
        return sbf.toString();
    }

    private String getMethods(Set<RequestMethod> requestMethods) {
        StringBuilder sbf = new StringBuilder();
        for (RequestMethod requestMethod : requestMethods) {
            sbf.append(requestMethod.toString()).append(StrUtil.COMMA);
        }
        if (requestMethods.size() > 0) {
            sbf.deleteCharAt(sbf.length() - 1);
        }
        return sbf.toString();
    }


}
