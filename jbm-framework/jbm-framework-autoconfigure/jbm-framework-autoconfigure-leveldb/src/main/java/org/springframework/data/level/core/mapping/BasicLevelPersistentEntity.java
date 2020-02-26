package org.springframework.data.level.core.mapping;

import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.level.core.LevelPersistentProperty;
import org.springframework.data.level.core.TimeToLive;
import org.springframework.data.level.core.TimeToLiveAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class BasicLevelPersistentEntity<T> extends BasicKeyValuePersistentEntity<T, LevelPersistentProperty>
		implements LevelPersistentEntity<T> {

	private final TimeToLiveAccessor timeToLiveAccessor;

	/**
	 * Creates new {@link BasicLevelPersistentEntity}.
	 *
	 * @param information              must not be {@literal null}.
	 * @param fallbackKeySpaceResolver can be {@literal null}.
	 * @param timeToLiveAccessor       can be {@literal null}.
	 */
	public BasicLevelPersistentEntity(TypeInformation<T> information,
			@Nullable KeySpaceResolver fallbackKeySpaceResolver, TimeToLiveAccessor timeToLiveAccessor) {
		super(information, fallbackKeySpaceResolver);

		Assert.notNull(timeToLiveAccessor, "TimeToLiveAccessor must not be null");
		this.timeToLiveAccessor = timeToLiveAccessor;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.result.Level.core.mapping.LevelPersistentEntity#
	 * getTimeToLiveAccessor()
	 */
	@Override
	public TimeToLiveAccessor getTimeToLiveAccessor() {
		return this.timeToLiveAccessor;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.result.Level.core.mapping.LevelPersistentEntity#
	 * hasExplictTimeToLiveProperty()
	 */
	@Override
	public boolean hasExplictTimeToLiveProperty() {
		return getExplicitTimeToLiveProperty() != null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.result.Level.core.mapping.LevelPersistentEntity#
	 * getExplicitTimeToLiveProperty()
	 */
	@Override
	@Nullable
	public LevelPersistentProperty getExplicitTimeToLiveProperty() {
		return this.getPersistentProperty(TimeToLive.class);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.result.mapping.model.BasicPersistentEntity#
	 * returnPropertyIfBetterIdPropertyCandidateOrNull(org.springframework.result.
	 * mapping.PersistentProperty)
	 */
	@Override
	@Nullable
	protected LevelPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
			LevelPersistentProperty property) {

		Assert.notNull(property, "Property must not be null!");

		if (!property.isIdProperty()) {
			return null;
		}

		LevelPersistentProperty currentIdProperty = getIdProperty();
		boolean currentIdPropertyIsSet = currentIdProperty != null;

		if (!currentIdPropertyIsSet) {
			return property;
		}

		boolean currentIdPropertyIsExplicit = currentIdProperty.isAnnotationPresent(Id.class);
		boolean newIdPropertyIsExplicit = property.isAnnotationPresent(Id.class);

		if (currentIdPropertyIsExplicit && newIdPropertyIsExplicit) {
			throw new MappingException(String.format(
					"Attempt to add explicit id property %s but already have an property %s registered "
							+ "as explicit id. Check your mapping configuration!",
					property.getField(), currentIdProperty.getField()));
		}

		if (!currentIdPropertyIsExplicit && !newIdPropertyIsExplicit) {
			throw new MappingException(String.format(
					"Attempt to add id property %s but already have an property %s registered "
							+ "as id. Check your mapping configuration!",
					property.getField(), currentIdProperty.getField()));
		}

		if (newIdPropertyIsExplicit) {
			return property;
		}

		return null;
	}
}
