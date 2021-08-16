package org.springframework.data.level.serializer;

public interface LevelSerializer<T> {
	/**
	 * Serialize the given object to binary result.
	 *
	 * @param t
	 *            object to serialize
	 * @return the equivalent binary result
	 */
	byte[] serialize(T t) throws SerializationException;

	/**
	 * Deserialize an object from the given binary result.
	 *
	 * @param bytes
	 *            object binary representation
	 * @return the equivalent object instance
	 */
	T deserialize(byte[] bytes) throws SerializationException;
}
