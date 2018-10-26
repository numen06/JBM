package org.springframework.data.level.core.index;

public interface PathBasedRedisIndexDefinition extends IndexDefinition {

	/**
	 * @return can be {@literal null}.
	 */
	String getPath();

}