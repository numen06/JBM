package org.springframework.data.level.core;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

public class LevelPersistentProperty extends KeyValuePersistentProperty<LevelPersistentProperty> {

	private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<>();

	static {
		SUPPORTED_ID_PROPERTY_NAMES.add("id");
	}

	/**
	 * Creates new {@link LevelPersistentProperty}.
	 *
	 * @param property
	 * @param owner
	 * @param simpleTypeHolder
	 */
	public LevelPersistentProperty(Property property, PersistentEntity<?, LevelPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#
	 * isIdProperty()
	 */
	@Override
	public boolean isIdProperty() {

		if (super.isIdProperty()) {
			return true;
		}

		return SUPPORTED_ID_PROPERTY_NAMES.contains(getName());
	}
}
