package com.td.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.td.util.bean.MethodWrap;

/**
 * 
 * @author wesley
 *
 */
public class AnnotatedUtils {

	/**
	 * 通过注解找到相应的方法
	 * 
	 * @param listener
	 * @param targerClass
	 * @return
	 */
	public static ImmutableList<Method> findAllMethods(Object listener, Class<? extends Annotation> targerClass) {
		Class<?> clazz = listener.getClass();
		return getAnnotatedMethods(clazz, targerClass);
	}

	/**
	 * 获取注解所有方法
	 * 
	 * @param listener
	 * @param targerClass
	 * @return
	 */
	public static Multimap<Class<?>, MethodWrap> findAllParameters(Object listener, Class<? extends Annotation> targerClass) {
		Multimap<Class<?>, MethodWrap> methodsInListener = HashMultimap.create();
		Class<?> clazz = listener.getClass();
		for (Method method : getAnnotatedMethods(clazz, targerClass)) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			Class<?> eventType = parameterTypes[0];
			MethodWrap subscriber = makeSubscriber(listener, method);
			methodsInListener.put(eventType, subscriber);
		}
		return methodsInListener;
	}

	private static ImmutableList<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> targerClass) {
		try {
			return getAnnotatedMethodsInternal(clazz, targerClass);
		} catch (UncheckedExecutionException e) {
			throw Throwables.propagate(e.getCause());
		}
	}

	private static final class MethodIdentifier {
		private final String name;
		private final List<Class<?>> parameterTypes;

		MethodIdentifier(Method method) {
			this.name = method.getName();
			this.parameterTypes = Arrays.asList(method.getParameterTypes());
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(name, parameterTypes);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof MethodIdentifier) {
				MethodIdentifier ident = (MethodIdentifier) o;
				return name.equals(ident.name) && parameterTypes.equals(ident.parameterTypes);
			}
			return false;
		}
	}

	private static ImmutableList<Method> getAnnotatedMethodsInternal(Class<?> clazz, Class<? extends Annotation> targerClass) {
		Set<? extends Class<?>> supers = TypeToken.of(clazz).getTypes().rawTypes();
		Map<MethodIdentifier, Method> identifiers = Maps.newHashMap();
		for (Class<?> superClazz : supers) {
			for (Method superClazzMethod : superClazz.getMethods()) {
				if (superClazzMethod.isAnnotationPresent(targerClass)) {
//					Class<?>[] parameterTypes = superClazzMethod.getParameterTypes();
//					if (parameterTypes.length != 1) {
//						throw new IllegalArgumentException("Method " + superClazzMethod + " has @Annotation annotation, but requires " + parameterTypes.length
//							+ " arguments.  Annotation " + targerClass + " methods must require a single argument.");
//					}

					MethodIdentifier ident = new MethodIdentifier(superClazzMethod);
					if (!identifiers.containsKey(ident)) {
						identifiers.put(ident, superClazzMethod);
					}
				}
			}
		}
		return ImmutableList.copyOf(identifiers.values());
	}

	/**
	 * Creates an {@code MethodWrap} for subsequently calling {@code method} on
	 * {@code listener}. Selects an MethodWrap implementation based on the
	 * annotations on {@code method}.
	 *
	 * @param listener
	 *            object bearing the event subscriber method.
	 * @param method
	 *            the event subscriber method to wrap in an MethodWrap.
	 * @return an MethodWrap that will call {@code method} on {@code listener}
	 *         when invoked.
	 */
	private static MethodWrap makeSubscriber(Object listener, Method method) {
		MethodWrap wrapper = null;
		if (methodIsDeclaredThreadSafe(method)) {
			wrapper = new MethodWrap(listener, method);
		}
		return wrapper;
	}

	/**
	 * Checks whether {@code method} is thread-safe, as indicated by the
	 * {@link AllowConcurrentEvents} annotation.
	 *
	 * @param method
	 *            subscriber method to check.
	 * @return {@code true} if {@code subscriber} is marked as thread-safe,
	 *         {@code false} otherwise.
	 */
	private static boolean methodIsDeclaredThreadSafe(Method method) {
		return method.getAnnotation(AllowConcurrentEvents.class) != null;
	}
}
