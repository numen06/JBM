
package org.springframework.data.level.serializer;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.util.Assert;

/**
 * @author wesley.zhang
 *
 */
public class JdkSerializationLevelSerializer implements LevelSerializer<Object> {

	private final Converter<Object, byte[]> serializer;
	private final Converter<byte[], Object> deserializer;

	/**
	 * Creates a new {@link JdkSerializationLevelSerializer} using the default
	 * class loader.
	 */
	public JdkSerializationLevelSerializer() {
		this(new SerializingConverter(), new DeserializingConverter());
	}

	/**
	 * Creates a new {@link JdkSerializationLevelSerializer} using a
	 * {@link ClassLoader}.
	 *
	 * @param classLoader
	 * @since 1.7
	 */
	public JdkSerializationLevelSerializer(ClassLoader classLoader) {
		this(new SerializingConverter(), new DeserializingConverter(classLoader));
	}

	/**
	 * Creates a new {@link JdkSerializationLevelSerializer} using a
	 * {@link Converter converters} to serialize and deserialize objects.
	 * 
	 * @param serializer
	 *            must not be {@literal null}
	 * @param deserializer
	 *            must not be {@literal null}
	 * @since 1.7
	 */
	public JdkSerializationLevelSerializer(Converter<Object, byte[]> serializer, Converter<byte[], Object> deserializer) {

		Assert.notNull("Serializer must not be null!");
		Assert.notNull("Deserializer must not be null!");

		this.serializer = serializer;
		this.deserializer = deserializer;
	}

	public Object deserialize(byte[] bytes) {
		if (SerializationUtils.isEmpty(bytes)) {
			return null;
		}

		try {
			return deserializer.convert(bytes);
		} catch (Exception ex) {
			throw new SerializationException("Cannot deserialize", ex);
		}
	}

	public byte[] serialize(Object object) {
		if (object == null) {
			return SerializationUtils.EMPTY_ARRAY;
		}
		try {
			return serializer.convert(object);
		} catch (Exception ex) {
			throw new SerializationException("Cannot serialize", ex);
		}
	}
}
