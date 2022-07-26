package org.springframework.data.level.core.mapping;

import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.level.core.LevelPersistentProperty;
import org.springframework.data.level.core.TimeToLiveAccessor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

public interface LevelPersistentEntity<T> extends KeyValuePersistentEntity<T, LevelPersistentProperty> {

    /**
     * Get the {@link TimeToLiveAccessor} associated with the entity.
     *
     * @return never {@literal null}.
     */
    TimeToLiveAccessor getTimeToLiveAccessor();

    /**
     * @return {@literal true} when a property is annotated with
     * {@link org.springframework.data.Level.core.TimeToLive}.
     * @since 1.8
     */
    boolean hasExplictTimeToLiveProperty();

    /**
     * Get the {@link PersistentProperty} that is annotated with
     * {@link org.springframework.data.Level.core.TimeToLive}.
     *
     * @return can be {@null}.
     * @since 1.8
     */
    @Nullable
    LevelPersistentProperty getExplicitTimeToLiveProperty();
}
