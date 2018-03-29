package com.td.framework.serialization;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.ClassUtils;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.kryo.Kryo;
import com.td.util.ArrayUtils;

public class Kryox extends Kryo {

	private final ConcurrentHashMap<Class<?>, Constructor<?>> _constructors = new ConcurrentHashMap<Class<?>, Constructor<?>>();

	@Override
	public <T> T newInstance(Class<T> type) {
		try {
			return super.newInstance(type);
		} catch (Exception e) {
			return (T) newInstanceFromReflectionFactory(type);
		}
	}

	public <T> T newInstanceFromReflectionFactory(Class<T> type) {
		Constructor<?> constructor = _constructors.get(type);
		if (constructor == null) {
			return newConstructorForSerialization(type);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> T newConstructorForSerialization(Class<T> type) {
		Class<?>[] pageTypes = null;
		Constructor<?> constructor = null;
		int i = 0;
		while (constructor == null) {
			try {
				pageTypes = ArrayUtils.newArray(Class.class, i);
				Arrays.fill(pageTypes, Object.class);
				i++;
				constructor = ClassUtils.getConstructorIfAvailable(type, pageTypes);
				System.out.println(JSON.toJSONString(constructor.getParameterTypes()));
				constructor.setAccessible(true);
				return (T) constructor.newInstance(new Object());
			} catch (Exception e) {
				continue;
			}
		}
		return null;

	}

}