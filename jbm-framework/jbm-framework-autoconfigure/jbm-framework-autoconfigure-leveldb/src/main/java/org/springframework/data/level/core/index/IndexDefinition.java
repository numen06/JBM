package org.springframework.data.level.core.index;

import org.springframework.data.util.TypeInformation;

import java.util.Collection;

public interface IndexDefinition {
    /**
     * @return never {@literal null}.
     */
    String getKeyspace();

    /**
     * @return never {@literal null}.
     */
    Collection<Condition<?>> getConditions();

    /**
     * @return never {@literal null}.
     */
    IndexValueTransformer valueTransformer();

    /**
     * @return never {@literal null}.
     */
    String getIndexName();

    /**
     * @param <T>
     * @author Christoph Strobl
     * @since 1.7
     */
    public static interface Condition<T> {
        boolean matches(T value, IndexingContext context);
    }

    /**
     * Context in which a particular value is about to get indexed.
     *
     * @author Christoph Strobl
     * @since 1.7
     */
    public class IndexingContext {

        private final String keyspace;
        private final String path;
        private final TypeInformation<?> typeInformation;

        public IndexingContext(String keyspace, String path, TypeInformation<?> typeInformation) {

            this.keyspace = keyspace;
            this.path = path;
            this.typeInformation = typeInformation;
        }

        public String getKeyspace() {
            return keyspace;
        }

        public String getPath() {
            return path;
        }

        public TypeInformation<?> getTypeInformation() {
            return typeInformation;
        }
    }
}
