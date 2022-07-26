package org.springframework.data.level;

import org.springframework.data.keyvalue.core.KeyValueTemplate;

/**
 * @author wesley
 */
public class LevelKeyValueTemplate extends KeyValueTemplate {
    /**
     * Create new {@link LevelKeyValueTemplate}.
     *
     * @param adapter        must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public LevelKeyValueTemplate(LevelKeyValueAdapter adapter, LevelMappingContext mappingContext) {
        super(adapter, mappingContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.keyvalue.core.KeyValueTemplate#getMappingContext
     * ()
     */
    @Override
    public LevelMappingContext getMappingContext() {
        return (LevelMappingContext) super.getMappingContext();
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
    }

}
