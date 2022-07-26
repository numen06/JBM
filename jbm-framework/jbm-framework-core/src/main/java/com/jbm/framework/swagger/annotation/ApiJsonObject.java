package com.jbm.framework.swagger.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-17 22:38
 **/
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiJsonObject {

    ApiJsonProperty[] propertys() default {}; //对象属性值

//    String name() default "body";  //对象名称

    Class type();


}

