package org.springframework.data.level.core.index;

import java.io.Serializable;
import java.util.Set;

public interface IndexDefinitionProvider {

	/**
	 * Gets all of the {@link RedisIndexSetting} for a given keyspace.
	 *
	 * @param keyspace
	 *            the keyspace to get
	 * @return never {@literal null}
	 */
	boolean hasIndexFor(Serializable keyspace);

	/**
	 * Checks if an index is defined for a given keyspace and property path.
	 *
	 * @param keyspace
	 * @param path
	 * @return true if index is defined.
	 */
	boolean hasIndexFor(Serializable keyspace, String path);

	/**
	 * Get the list of {@link IndexDefinition} for a given keyspace.
	 *
	 * @param keyspace
	 * @return never {@literal null}.
	 */
	Set<IndexDefinition> getIndexDefinitionsFor(Serializable keyspace);

	/**
	 * Get the list of {@link IndexDefinition} for a given keyspace and property
	 * path.
	 *
	 * @param keyspace
	 * @param path
	 * @return never {@literal null}.
	 */
	Set<IndexDefinition> getIndexDefinitionsFor(Serializable keyspace, String path);
}
