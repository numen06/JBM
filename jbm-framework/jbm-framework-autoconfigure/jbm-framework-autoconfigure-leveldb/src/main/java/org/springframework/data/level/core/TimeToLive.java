package org.springframework.data.level.core;

import org.springframework.data.annotation.ReadOnlyProperty;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(value = {ElementType.FIELD, ElementType.METHOD})
@ReadOnlyProperty
public @interface TimeToLive {

    /**
     * {@link TimeUnit} unit to use.
     *
     * @return {@link TimeUnit#SECONDS} by default.
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}