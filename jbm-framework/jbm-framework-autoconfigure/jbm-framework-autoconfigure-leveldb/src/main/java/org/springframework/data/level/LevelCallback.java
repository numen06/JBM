package org.springframework.data.level;

import org.iq80.leveldb.DB;
import org.springframework.dao.DataAccessException;
import org.springframework.data.level.serializer.LevelSerializer;

public interface LevelCallback<T> {
    @SuppressWarnings("rawtypes")
    T doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException;
}
