package jbm.framework.boot.autoconfigure.dictionary.cache;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

public class BeanPersistentProperty<P extends KeyValuePersistentProperty<P>> extends KeyValuePersistentProperty<P> {

	public BeanPersistentProperty(Property property, PersistentEntity<?, P> owner, SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
		// TODO Auto-generated constructor stub
	}

	private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<String>();

	static {
		SUPPORTED_ID_PROPERTY_NAMES.add("id");
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
