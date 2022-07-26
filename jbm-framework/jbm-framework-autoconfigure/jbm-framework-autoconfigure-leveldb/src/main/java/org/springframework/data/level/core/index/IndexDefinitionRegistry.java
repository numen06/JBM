package org.springframework.data.level.core.index;

public interface IndexDefinitionRegistry {

    /**
     * Add given {@link RedisIndexSetting}.
     *
     * @param indexDefinition must not be {@literal null}.
     */
    void addIndexDefinition(IndexDefinition indexDefinition);
}
