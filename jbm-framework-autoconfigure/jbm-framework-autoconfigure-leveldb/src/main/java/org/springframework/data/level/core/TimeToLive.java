package org.springframework.data.level.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.springframework.data.annotation.ReadOnlyProperty;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(value = { ElementType.FIELD, ElementType.METHOD })
@ReadOnlyProperty
public @interface TimeToLive {

	/**
	 * {@link TimeUnit} unit to use.
	 * 
	 * @return {@link TimeUnit#SECONDS} by default.
	 */
	TimeUnit unit() default TimeUnit.SECONDS;
}