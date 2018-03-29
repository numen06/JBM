package org.springframework.data.level.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.keyvalue.annotation.KeySpace;

@Persistent
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE })
@KeySpace
public @interface LevelHash {

	/**
	 * The prefix to distinguish between domain types.
	 * 
	 * @return
	 * @see KeySpace
	 */
	@AliasFor(annotation = KeySpace.class, attribute = "value")
	String value() default "";

	/**
	 * Time before expire in seconds. Superseded by {@link TimeToLive}.
	 * 
	 * @return positive number when expiration should be applied.
	 */
	long timeToLive() default -1L;

}