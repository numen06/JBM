package com.jbm.framework.boot.autoconfigure.retrofit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wesley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ApiPlatform {

    String name() ;

    Class<? extends BaseStrategy>[] strategys();


}
