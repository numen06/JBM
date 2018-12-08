package org.springframework.data.level.convent;

import org.springframework.data.level.core.index.ConfigurableIndexDefinitionProvider;

public class MappingConfiguration {
	private final ConfigurableIndexDefinitionProvider indexConfiguration;
	private final KeyspaceConfiguration keyspaceConfiguration;

	/**
	 * Creates new {@link MappingConfiguration}.
	 * 
	 * @param indexConfiguration
	 *            must not be {@literal null}.
	 * @param keyspaceConfiguration
	 *            must not be {@literal null}.
	 */
	public MappingConfiguration(ConfigurableIndexDefinitionProvider indexConfiguration, KeyspaceConfiguration keyspaceConfiguration) {
		this.indexConfiguration = indexConfiguration;
		this.keyspaceConfiguration = keyspaceConfiguration;
	}

	/**
	 * @return never {@literal null}.
	 */
	public ConfigurableIndexDefinitionProvider getIndexConfiguration() {
		return indexConfiguration;
	}

	/**
	 * @return never {@literal null}.
	 */
	public KeyspaceConfiguration getKeyspaceConfiguration() {
		return keyspaceConfiguration;
	}
}
