package org.springframework.data.level.core.mapping;

import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.level.core.TimeToLiveAccessor;

public interface LevelPersistentEntity<T> extends KeyValuePersistentEntity<T> {

	TimeToLiveAccessor getTimeToLiveAccessor();

}
