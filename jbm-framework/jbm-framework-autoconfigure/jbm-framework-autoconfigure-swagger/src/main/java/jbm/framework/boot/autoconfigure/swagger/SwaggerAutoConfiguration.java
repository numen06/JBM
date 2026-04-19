package jbm.framework.boot.autoconfigure.swagger;

import io.swagger.annotations.Api;
import jbm.framework.boot.autoconfigure.swagger.config.SwaggerBeanPostProcessor;
import jbm.framework.boot.autoconfigure.swagger.config.SwaggerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

//@Configuration
@EnableSwagger2
//@EnableAutoConfiguration
@ConditionalOnProperty(name = "swagger2.enabled", matchIfMissing = true)
public class SwaggerAutoConfiguration {
    /**
     * 默认的排除路径，排除Spring Boot默认的错误处理路径和端点
     */
    private static final List<String> DEFAULT_EXCLUDE_PATH = Arrays.asList("/error", "/actuator/**");

    private static final String BASE_PATH = "/**";

    /**
     * 抑制 Swagger/SpringDoc 的警告日志
     * 这些警告通常是由于 @ApiImplicitParam 注解缺少 dataType 属性导致的
     * 警告日志中的类名可能被缩短（如 s.d.b.FormParameterSpecificationProvider）
     * 
     * 注意：实际的日志抑制需要在 logback-spring.xml 或 application.yml 中配置
     * 这里提供一个通用的方法，如果使用 Logback，会尝试设置日志级别
     */
    @PostConstruct
    public void suppressSwaggerWarnings() {
        try {
            // 尝试使用 Logback 设置日志级别为 ERROR，抑制 WARN 级别的警告
            org.slf4j.Logger logger = LoggerFactory.getLogger("springfox.documentation.spring.web.readers.parameter.FormParameterSpecificationProvider");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.ERROR);
            }
            
            logger = LoggerFactory.getLogger("springfox.documentation.spring.web.readers.operation.OperationImplicitParameterReader");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.ERROR);
            }
            
            // 如果使用了 SpringDoc，也抑制相关警告
            logger = LoggerFactory.getLogger("springdoc.documentation.builder.FormParameterSpecificationProvider");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.ERROR);
            }
            
            logger = LoggerFactory.getLogger("springdoc.documentation.swagger.readers.operation.OperationImplicitParameterReader");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.ERROR);
            }
        } catch (Exception e) {
            // 如果 Logback 不可用或类不存在，忽略异常（可能使用了其他日志框架）
            // 建议在 logback-spring.xml 或 application.yml 中配置日志级别来抑制这些警告
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public SwaggerProperties swaggerProperties() {
        return new SwaggerProperties();
    }

    @Bean
    public Docket docket(SwaggerProperties swaggerProperties) {
        // base-path处理
        if (swaggerProperties.getBasePath().isEmpty()) {
            swaggerProperties.getBasePath().add(BASE_PATH);
        }
        // noinspection unchecked
        List<Predicate<String>> basePath = new ArrayList<Predicate<String>>();
        swaggerProperties.getBasePath().forEach(path -> basePath.add(PathSelectors.ant(path)));

        // exclude-path处理
        if (swaggerProperties.getExcludePath().isEmpty()) {
            swaggerProperties.getExcludePath().addAll(DEFAULT_EXCLUDE_PATH);
        }

        List<Predicate<String>> excludePath = new ArrayList<>();
        swaggerProperties.getExcludePath().forEach(path -> excludePath.add(PathSelectors.ant(path)));

        ApiSelectorBuilder builder = new Docket(DocumentationType.SWAGGER_2)
                .host(swaggerProperties.getHost())
                .apiInfo(apiInfo(swaggerProperties)).select()
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class));
//                .apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()));

        swaggerProperties.getBasePath().forEach(p -> builder.paths(PathSelectors.ant(p)));
        swaggerProperties.getExcludePath().forEach(p -> builder.paths(PathSelectors.ant(p).negate()));

        return builder.build().securitySchemes(securitySchemes()).securityContexts(securityContexts()).pathMapping("/");
    }

    /**
     * 安全模式，这里指定token通过Authorization头请求头传递
     */
    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> apiKeyList = new ArrayList<SecurityScheme>();
        apiKeyList.add(new ApiKey("Authorization", "Authorization", "header"));
        return apiKeyList;
    }

    /**
     * 安全上下文
     */
    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .operationSelector(o -> o.requestMappingPattern().matches("/.*"))
                        .build());
        return securityContexts;
    }

    /**
     * 默认的全局鉴权策略
     *
     * @return
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        return securityReferences;
    }

    private ApiInfo apiInfo(SwaggerProperties swaggerProperties) {
        return new ApiInfoBuilder()
                .title(swaggerProperties.getTitle())
                .description(swaggerProperties.getDescription())
                .license(swaggerProperties.getLicense())
                .licenseUrl(swaggerProperties.getLicenseUrl())
                .termsOfServiceUrl(swaggerProperties.getTermsOfServiceUrl())
                .contact(new Contact(swaggerProperties.getContact().getName(), swaggerProperties.getContact().getUrl(), swaggerProperties.getContact().getEmail()))
                .version(swaggerProperties.getVersion())
                .build();
    }

    @Bean
    public SwaggerBeanPostProcessor swaggerBeanPostProcessor() {
        return new SwaggerBeanPostProcessor();
    }

}
