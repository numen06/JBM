package org.springframework.data.level;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.level.convent.KeyspaceConfiguration;
import org.springframework.data.level.convent.KeyspaceConfiguration.KeyspaceSettings;
import org.springframework.data.level.convent.MappingConfiguration;
import org.springframework.data.level.core.LevelHash;
import org.springframework.data.level.core.LevelPersistentProperty;
import org.springframework.data.level.core.TimeToLive;
import org.springframework.data.level.core.TimeToLiveAccessor;
import org.springframework.data.level.core.index.IndexConfiguration;
import org.springframework.data.level.core.mapping.BasicLevelPersistentEntity;
import org.springframework.data.level.core.mapping.LevelPersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

public class LevelMappingContext extends KeyValueMappingContext {
	private final MappingConfiguration mappingConfiguration;
	private final TimeToLiveAccessor timeToLiveAccessor;

	private KeySpaceResolver fallbackKeySpaceResolver;

	/**
	 * Creates new {@link LevelMappingContext} with empty
	 * {@link MappingConfiguration}.
	 */
	public LevelMappingContext() {
		this(new MappingConfiguration(new IndexConfiguration(), new KeyspaceConfiguration()));
	}

	/**
	 * Creates new {@link LevelMappingContext}.
	 * 
	 * @param mappingConfiguration
	 *            can be {@literal null}.
	 */
	public LevelMappingContext(MappingConfiguration mappingConfiguration) {

		this.mappingConfiguration = mappingConfiguration != null ? mappingConfiguration : new MappingConfiguration(new IndexConfiguration(), new KeyspaceConfiguration());

		setFallbackKeySpaceResolver(new ConfigAwareKeySpaceResolver(this.mappingConfiguration.getKeyspaceConfiguration()));
		this.timeToLiveAccessor = new ConfigAwareTimeToLiveAccessor(this.mappingConfiguration.getKeyspaceConfiguration(), this);
	}

	/**
	 * Configures the {@link KeySpaceResolver} to be used if not explicit key
	 * space is annotated to the domain type.
	 * 
	 * @param fallbackKeySpaceResolver
	 *            can be {@literal null}.
	 */
	public void setFallbackKeySpaceResolver(KeySpaceResolver fallbackKeySpaceResolver) {
		this.fallbackKeySpaceResolver = fallbackKeySpaceResolver;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> LevelPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new BasicLevelPersistentEntity<T>(typeInformation, fallbackKeySpaceResolver, timeToLiveAccessor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * getPersistentEntity(java.lang.Class)
	 */
	@Override
	public LevelPersistentEntity<?> getPersistentEntity(Class<?> type) {
		KeyValuePersistentEntity<?> kkk = super.getPersistentEntity(type);
		if (kkk == null) {
			kkk = this.createPersistentEntity(ClassTypeInformation.from(type));
			this.addPersistentEntity(type);
		}
		return (LevelPersistentEntity<?>) kkk;
	}

	@Override
	protected KeyValuePersistentEntity<?> addPersistentEntity(Class<?> type) {
		return super.addPersistentEntity(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * getPersistentEntity(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public LevelPersistentEntity<?> getPersistentEntity(KeyValuePersistentProperty persistentProperty) {
		return (LevelPersistentEntity<?>) super.getPersistentEntity(persistentProperty);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * getPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	public LevelPersistentEntity<?> getPersistentEntity(TypeInformation<?> type) {
		return (LevelPersistentEntity<?>) super.getPersistentEntity(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.keyvalue.core.mapping.context.
	 * KeyValueMappingContext#createPersistentProperty(java.lang.reflect.Field,
	 * java.beans.PropertyDescriptor,
	 * org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity,
	 * org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected KeyValuePersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor, KeyValuePersistentEntity<?> owner,
		SimpleTypeHolder simpleTypeHolder) {
		return new LevelPersistentProperty(field, descriptor, owner, simpleTypeHolder);
	}

	/**
	 * Get the {@link MappingConfiguration} used.
	 * 
	 * @return never {@literal null}.
	 */
	public MappingConfiguration getMappingConfiguration() {
		return mappingConfiguration;
	}

	/**
	 * {@link KeySpaceResolver} implementation considering {@link KeySpace} and
	 * {@link KeyspaceConfiguration}.
	 * 
	 * @author Christoph Strobl
	 * @since 1.7
	 */
	static class ConfigAwareKeySpaceResolver implements KeySpaceResolver {

		private final KeyspaceConfiguration keyspaceConfig;

		public ConfigAwareKeySpaceResolver(KeyspaceConfiguration keyspaceConfig) {

			this.keyspaceConfig = keyspaceConfig;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.keyvalue.core.mapping.KeySpaceResolver#
		 * resolveKeySpace(java.lang.Class)
		 */
		@Override
		public String resolveKeySpace(Class<?> type) {

			Assert.notNull(type, "Type must not be null!");
			if (keyspaceConfig.hasSettingsFor(type)) {

				String value = keyspaceConfig.getKeyspaceSettings(type).getKeyspace();
				if (StringUtils.hasText(value)) {
					return value;
				}
			}

			return ClassNameKeySpaceResolver.INSTANCE.resolveKeySpace(type);
		}
	}

	/**
	 * {@link KeySpaceResolver} implementation considering {@link KeySpace}.
	 * 
	 * @author Christoph Strobl
	 * @since 1.7
	 */
	enum ClassNameKeySpaceResolver implements KeySpaceResolver {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.keyvalue.core.KeySpaceResolver#
		 * resolveKeySpace(java.lang.Class)
		 */
		@Override
		public String resolveKeySpace(Class<?> type) {

			Assert.notNull(type, "Type must not be null!");
			return ClassUtils.getUserClass(type).getName();
		}
	}

	/**
	 * {@link TimeToLiveAccessor} implementation considering
	 * {@link KeyspaceConfiguration}.
	 * 
	 * @author Christoph Strobl
	 * @since 1.7
	 */
	static class ConfigAwareTimeToLiveAccessor implements org.springframework.data.level.core.TimeToLiveAccessor {

		private final Map<Class<?>, Long> defaultTimeouts;
		private final Map<Class<?>, PersistentProperty<?>> timeoutProperties;
		private final Map<Class<?>, Method> timeoutMethods;
		private final org.springframework.data.level.convent.KeyspaceConfiguration keyspaceConfig;
		private final LevelMappingContext mappingContext;

		/**
		 * Creates new {@link ConfigAwareTimeToLiveAccessor}
		 * 
		 * @param keyspaceConfig
		 *            must not be {@literal null}.
		 * @param mappingContext
		 *            must not be {@literal null}.
		 */
		ConfigAwareTimeToLiveAccessor(KeyspaceConfiguration keyspaceConfig, LevelMappingContext mappingContext) {

			Assert.notNull(keyspaceConfig, "KeyspaceConfiguration must not be null!");
			Assert.notNull(mappingContext, "MappingContext must not be null!");

			this.defaultTimeouts = new HashMap<Class<?>, Long>();
			this.timeoutProperties = new HashMap<Class<?>, PersistentProperty<?>>();
			this.timeoutMethods = new HashMap<Class<?>, Method>();
			this.keyspaceConfig = keyspaceConfig;
			this.mappingContext = mappingContext;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.redis.core.TimeToLiveResolver#
		 * resolveTimeToLive(java.lang.Object)
		 */
		@Override
		@SuppressWarnings({ "rawtypes" })
		public Long getTimeToLive(final Object source) {

			Assert.notNull(source, "Source must not be null!");
			Class<?> type = source instanceof Class<?> ? (Class<?>) source : source.getClass();

			Long defaultTimeout = resolveDefaultTimeOut(type);
			TimeUnit unit = TimeUnit.SECONDS;

			PersistentProperty<?> ttlProperty = resolveTtlProperty(type);

			if (ttlProperty != null) {

				if (ttlProperty.findAnnotation(TimeToLive.class) != null) {
					unit = ttlProperty.findAnnotation(TimeToLive.class).unit();
				}

				LevelPersistentEntity entity = mappingContext.getPersistentEntity(type);
				Long timeout = (Long) entity.getPropertyAccessor(source).getProperty(ttlProperty);
				if (timeout != null) {
					return TimeUnit.SECONDS.convert(timeout, unit);
				}
			} else {

				Method timeoutMethod = resolveTimeMethod(type);
				if (timeoutMethod != null) {

					TimeToLive ttl = AnnotationUtils.findAnnotation(timeoutMethod, TimeToLive.class);
					try {
						Number timeout = (Number) timeoutMethod.invoke(source);
						if (timeout != null) {
							return TimeUnit.SECONDS.convert(timeout.longValue(), ttl.unit());
						}
					} catch (IllegalAccessException e) {
						throw new IllegalStateException("Not allowed to access method '" + timeoutMethod.getName() + "': " + e.getMessage(), e);
					} catch (IllegalArgumentException e) {
						throw new IllegalStateException("Cannot invoke method '" + timeoutMethod.getName() + " without arguments': " + e.getMessage(), e);
					} catch (InvocationTargetException e) {
						throw new IllegalStateException("Cannot access method '" + timeoutMethod.getName() + "': " + e.getMessage(), e);
					}
				}
			}

			return defaultTimeout;
		}

		private Long resolveDefaultTimeOut(Class<?> type) {

			if (this.defaultTimeouts.containsKey(type)) {
				return defaultTimeouts.get(type);
			}

			Long defaultTimeout = null;

			if (keyspaceConfig.hasSettingsFor(type)) {
				defaultTimeout = keyspaceConfig.getKeyspaceSettings(type).getTimeToLive();
			}

			LevelHash hash = mappingContext.getPersistentEntity(type).findAnnotation(LevelHash.class);
			if (hash != null && hash.timeToLive() > 0) {
				defaultTimeout = hash.timeToLive();
			}

			defaultTimeouts.put(type, defaultTimeout);
			return defaultTimeout;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private PersistentProperty<?> resolveTtlProperty(Class<?> type) {

			if (timeoutProperties.containsKey(type)) {
				return timeoutProperties.get(type);
			}

			LevelPersistentEntity entity = mappingContext.getPersistentEntity(type);
			PersistentProperty<?> ttlProperty = entity.getPersistentProperty(TimeToLive.class);

			if (ttlProperty != null) {

				timeoutProperties.put(type, ttlProperty);
				return ttlProperty;
			}

			if (keyspaceConfig.hasSettingsFor(type)) {

				KeyspaceSettings settings = keyspaceConfig.getKeyspaceSettings(type);
				if (StringUtils.hasText(settings.getTimeToLivePropertyName())) {

					ttlProperty = entity.getPersistentProperty(settings.getTimeToLivePropertyName());
					timeoutProperties.put(type, ttlProperty);
					return ttlProperty;
				}
			}

			timeoutProperties.put(type, null);
			return null;
		}

		private Method resolveTimeMethod(final Class<?> type) {

			if (timeoutMethods.containsKey(type)) {
				return timeoutMethods.get(type);
			}

			timeoutMethods.put(type, null);
			ReflectionUtils.doWithMethods(type, new MethodCallback() {

				@Override
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					timeoutMethods.put(type, method);
				}
			}, new MethodFilter() {

				@Override
				public boolean matches(Method method) {
					return ClassUtils.isAssignable(Number.class, method.getReturnType()) && AnnotationUtils.findAnnotation(method, TimeToLive.class) != null;
				}
			});

			return timeoutMethods.get(type);
		}
	}
}
