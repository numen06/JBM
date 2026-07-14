package com.jbm.cluster.common.feign.annotation;

import com.jbm.cluster.common.feign.FeignTokenContext;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Relays the current inbound Authorization token to this Feign request.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(headers = FeignTokenContext.ACCESS_MODE_HEADER + "=" + FeignTokenContext.ACCESS_MODE_RELAY)
public @interface FeignTokenRelay {
}
