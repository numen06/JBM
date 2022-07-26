package org.springframework.data.level;

import org.iq80.leveldb.WriteBatch;
import org.springframework.dao.DataAccessException;

public interface LevelBatchCallback<T> {
    T doInLevel(WriteBatch batch) throws DataAccessException;
}
