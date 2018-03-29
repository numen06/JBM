package org.springframework.data.level.core.mapping;

import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.level.core.TimeToLiveAccessor;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

public class BasicLevelPersistentEntity<T> extends BasicKeyValuePersistentEntity<T> implements LevelPersistentEntity<T> {

	private TimeToLiveAccessor timeToLiveAccessor;

	/**
	 * Creates new {@link BasicLevelPersistentEntity}.
	 * 
	 * @param information
	 *            must not be {@literal null}.
	 * @param fallbackKeySpaceResolver
	 *            can be {@literal null}.
	 * @param timeToLiveResolver
	 *            can be {@literal null}.
	 */
	public BasicLevelPersistentEntity(TypeInformation<T> information, KeySpaceResolver fallbackKeySpaceResolver, TimeToLiveAccessor timeToLiveAccessor) {
		super(information, fallbackKeySpaceResolver);

		Assert.notNull(timeToLiveAccessor, "TimeToLiveAccessor must not be null");
		this.timeToLiveAccessor = timeToLiveAccessor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.redis.core.mapping.RedisPersistentEntity#
	 * getTimeToLiveAccessor()
	 */
	@Override
	public TimeToLiveAccessor getTimeToLiveAccessor() {
		return this.timeToLiveAccessor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#
	 * returnPropertyIfBetterIdPropertyCandidateOrNull(org.springframework.data.
	 * mapping.PersistentProperty)
	 */
	@Override
	protected KeyValuePersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(KeyValuePersistentProperty property) {

		Assert.notNull(property);

		if (!property.isIdProperty()) {
			return null;
		}

		KeyValuePersistentProperty currentIdProperty = getIdProperty();
		boolean currentIdPropertyIsSet = currentIdProperty != null;

		if (!currentIdPropertyIsSet) {
			return property;
		}

		boolean currentIdPropertyIsExplicit = currentIdProperty.isAnnotationPresent(Id.class);
		boolean newIdPropertyIsExplicit = property.isAnnotationPresent(Id.class);

		if (currentIdPropertyIsExplicit && newIdPropertyIsExplicit) {
			throw new MappingException(
				String.format("Attempt to add explicit id property %s but already have an property %s registered " + "as explicit id. Check your mapping configuration!",
					property.getField(), currentIdProperty.getField()));
		}

		if (!currentIdPropertyIsExplicit && !newIdPropertyIsExplicit) {
			throw new MappingException(String.format("Attempt to add id property %s but already have an property %s registered " + "as id. Check your mapping configuration!",
				property.getField(), currentIdProperty.getField()));
		}

		if (newIdPropertyIsExplicit) {
			return property;
		}

		return null;
	}
}
