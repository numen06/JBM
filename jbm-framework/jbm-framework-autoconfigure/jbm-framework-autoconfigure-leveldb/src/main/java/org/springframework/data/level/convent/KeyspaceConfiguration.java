package org.springframework.data.level.convent;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyspaceConfiguration {
    private Map<Class<?>, KeyspaceSettings> settingsMap;

    public KeyspaceConfiguration() {

        this.settingsMap = new ConcurrentHashMap<Class<?>, KeyspaceSettings>();
        for (KeyspaceSettings initial : initialConfiguration()) {
            settingsMap.put(initial.type, initial);
        }
    }

    /**
     * Check if specific {@link KeyspaceSettings} are available for given type.
     *
     * @param type must not be {@literal null}.
     * @return true if settings exist.
     */
    public boolean hasSettingsFor(Class<?> type) {

        Assert.notNull(type, "Type to lookup must not be null!");

        if (settingsMap.containsKey(type)) {

            if (settingsMap.get(type) instanceof DefaultKeyspaceSetting) {
                return false;
            }

            return true;
        }

        for (KeyspaceSettings assignment : settingsMap.values()) {
            if (assignment.inherit) {
                if (ClassUtils.isAssignable(assignment.type, type)) {
                    settingsMap.put(type, assignment.cloneFor(type));
                    return true;
                }
            }
        }

        settingsMap.put(type, new DefaultKeyspaceSetting(type));
        return false;
    }

    /**
     * Get the {@link KeyspaceSettings} for given type.
     *
     * @param type must not be {@literal null}
     * @return {@literal null} if no settings configured.
     */
    public KeyspaceSettings getKeyspaceSettings(Class<?> type) {

        if (!hasSettingsFor(type)) {
            return null;
        }

        KeyspaceSettings settings = settingsMap.get(type);
        if (settings == null || settings instanceof DefaultKeyspaceSetting) {
            return null;
        }

        return settings;
    }

    /**
     * Customization hook.
     *
     * @return must not return {@literal null}.
     */
    protected Iterable<KeyspaceSettings> initialConfiguration() {
        return Collections.emptySet();
    }

    /**
     * Add {@link KeyspaceSettings} for type.
     *
     * @param keyspaceSettings must not be {@literal null}.
     */
    public void addKeyspaceSettings(KeyspaceSettings keyspaceSettings) {

        Assert.notNull(keyspaceSettings);
        this.settingsMap.put(keyspaceSettings.getType(), keyspaceSettings);
    }

    /**
     * @author Christoph Strobl
     * @since 1.7
     */
    public static class KeyspaceSettings {

        private final String keyspace;
        private final Class<?> type;
        private final boolean inherit;
        private Long timeToLive;
        private String timeToLivePropertyName;

        public KeyspaceSettings(Class<?> type, String keyspace) {
            this(type, keyspace, true);
        }

        public KeyspaceSettings(Class<?> type, String keyspace, boolean inherit) {

            this.type = type;
            this.keyspace = keyspace;
            this.inherit = inherit;
        }

        KeyspaceSettings cloneFor(Class<?> type) {
            return new KeyspaceSettings(type, this.keyspace, false);
        }

        public String getKeyspace() {
            return keyspace;
        }

        public Class<?> getType() {
            return type;
        }

        public Long getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(Long timeToLive) {
            this.timeToLive = timeToLive;
        }

        public String getTimeToLivePropertyName() {
            return timeToLivePropertyName;
        }

        public void setTimeToLivePropertyName(String propertyName) {
            timeToLivePropertyName = propertyName;
        }

    }

    /**
     * Marker class indicating no settings defined.
     *
     * @author Christoph Strobl
     * @since 1.7
     */
    private static class DefaultKeyspaceSetting extends KeyspaceSettings {

        public DefaultKeyspaceSetting(Class<?> type) {
            super(type, "#default#", false);
        }

    }

}
