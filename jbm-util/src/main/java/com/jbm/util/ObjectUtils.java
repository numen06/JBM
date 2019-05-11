package com.jbm.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jbm.util.map.SetHashMap;
import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 
 * 处理Object的工具类
 * 
 * @author Wesley
 * 
 */
public class ObjectUtils extends org.apache.commons.lang.ObjectUtils {

	/**
	 * 默认的Short:0
	 */
	public static final Short SHORT_DEF = 0;
	/**
	 * 默认的Float:0
	 */
	public static final Float FLOAT_DEF = 0f;
	/**
	 * 默认的Double:0
	 */
	public static final Double DOUBLE_DEF = 0d;
	/**
	 * 默认的Long:0
	 */
	public static final Long LONG_DEF = 0l;
	/**
	 * 默认的Number:0
	 */
	public static final Number NUMBER_DEF = 0;

	/**
	 * 是否为空
	 * 
	 * <pre>
	 * ObjectUtils.isNull("1", "2", null)  = true
	 * ObjectUtils.isNull("1", "2", "3")   = false
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	public static boolean isNull(Object... args) {
		for (Object o : args) {
			if (o == null)
				return true;
		}
		return false;
	}

	/**
	 * 
	 * 是否不为空
	 * 
	 * <pre>
	 * ObjectUtils.isNotNull("1", "2", null)  = false
	 * ObjectUtils.isNotNull("1", "2", "3")   = true
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	public static boolean isNotNull(Object... args) {
		return !isNull(args);
	}

	/**
	 * 存在多少个非NULL
	 * 
	 * @param args
	 * @return
	 */
	public static int hasNotNull(Object... args) {
		int index = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null)
				index++;
		}
		return index;
	}

	/**
	 * 存在多少个NULL
	 * 
	 * @param args
	 * @return
	 */
	public static int hasNull(Object... args) {
		int index = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				index++;
		}
		return index;
	}

	/**
	 * 全部不为空
	 * 
	 * <pre>
	 * ObjectUtils.allIsNotNull("1", "2", null)  = false
	 * ObjectUtils.allIsNotNull("1","2","3")     = true
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	public static boolean allIsNotNull(Object... args) {
		for (Object o : args) {
			if (o == null)
				return false;
		}
		return true;
	}

	/**
	 * 全部为空
	 * 
	 * <pre>
	 * ObjectUtils.allIsNull("1", "2", null)  = false
	 * ObjectUtils.allIsNull(null,null,null)  = true
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	public static boolean allIsNull(Object... args) {
		for (Object o : args) {
			if (o != null)
				return false;
		}
		return true;
	}

	/**
	 * 判断如果为空，则返回相应的默认值
	 * 
	 * <pre>
	 * ObjectUtils.nullToDefault(null,"test")   = test
	 * ObjectUtils.nullToDefault("test",null)   = null
	 * ObjectUtils.nullToDefault(1,"test")      = 1
	 * </pre>
	 * 
	 * @param object
	 * @param defaultValue
	 * @return
	 */
	public static <T> T nullToDefault(T object, T def) {
		return object == null ? def : object;
	}

	/**
	 * 克隆对象
	 * 
	 * @param o
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cloneObject(final T o) {
		return (T) org.apache.commons.lang.ObjectUtils.clone(o);
	}

	/**
	 * 对象匹配返回设定值
	 * 
	 * <pre>
	 * ObjectUtils.equalsToValue("1", "1", "2")  = 2
	 * ObjectUtils.equalsToValue(null, "1", "2") = 1
	 * </pre>
	 * 
	 * @param source
	 *            比较对象
	 * @param base
	 *            源对象
	 * @param result
	 *            返回值
	 * @return
	 */
	public static <T> T equalsToValue(T source, T base, T result) {
		return ObjectUtils.equals(source, base) ? result : source;
	}

	/**
	 * 对象不匹配返回设定值
	 * 
	 * <pre>
	 * ObjectUtils.notEqualsToValue("1", "1", "2")  = 1
	 * ObjectUtils.notEqualsToValue(null, "1", "2") = 2
	 * </pre>
	 * 
	 * @param source
	 *            比较对象
	 * @param base
	 *            源对象
	 * @param result
	 *            返回值
	 * @return
	 */
	public static <T> T notEqualsToValue(T source, T base, T result) {
		return ObjectUtils.equals(source, base) ? source : result;
	}

	/**
	 * 强制类型转换
	 * 
	 * <pre>
	 * [A extends B]
	 * source[A]能强转成target[B]返回[@param source]
	 * ObjectUtils.softCast(A, B)               = A
	 * source[B]能强转成target[A]返回[@param target]
	 * ObjectUtils.softCast(B, A)               = B
	 * 
	 * ObjectUtils.softCast(12312, "dsfads")    = dsfads
	 * ObjectUtils.softCast(null, "dsfads")     = dsfads
	 * ObjectUtils.softCast(1, 2)               = 1
	 * </pre>
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T softCast(Object source, T target) {
		T result = target;
		try {
			if (ObjectUtils.isNull(source, target))
				return target;
			if (target.getClass().isAssignableFrom(source.getClass()))
				result = (T) source;
			return result;
		} catch (ClassCastException c) {
			result = target;
		}
		return result;
	}

	/**
	 * Determine if the given objects are equal, returning {@code true} if both
	 * are {@code null} or {@code false} if only one is {@code null}.
	 * <p>
	 * Compares arrays with {@code Arrays.equals}, performing an equality check
	 * based on the array elements rather than the array reference.
	 * 
	 * @param o1
	 *            first Object to compare
	 * @param o2
	 *            second Object to compare
	 * @return whether the given objects are equal
	 * @see java.util.Arrays#equals
	 */
	public static boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			if (o1 instanceof Object[] && o2 instanceof Object[]) {
				return Arrays.equals((Object[]) o1, (Object[]) o2);
			}
			if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
				return Arrays.equals((boolean[]) o1, (boolean[]) o2);
			}
			if (o1 instanceof byte[] && o2 instanceof byte[]) {
				return Arrays.equals((byte[]) o1, (byte[]) o2);
			}
			if (o1 instanceof char[] && o2 instanceof char[]) {
				return Arrays.equals((char[]) o1, (char[]) o2);
			}
			if (o1 instanceof double[] && o2 instanceof double[]) {
				return Arrays.equals((double[]) o1, (double[]) o2);
			}
			if (o1 instanceof float[] && o2 instanceof float[]) {
				return Arrays.equals((float[]) o1, (float[]) o2);
			}
			if (o1 instanceof int[] && o2 instanceof int[]) {
				return Arrays.equals((int[]) o1, (int[]) o2);
			}
			if (o1 instanceof long[] && o2 instanceof long[]) {
				return Arrays.equals((long[]) o1, (long[]) o2);
			}
			if (o1 instanceof short[] && o2 instanceof short[]) {
				return Arrays.equals((short[]) o1, (short[]) o2);
			}
		}
		return false;
	}

	public static SetHashMap<String, Object> foreachObject(Object obj) {
		SetHashMap<String, Object> map = new SetHashMap<String, Object>();
		foreachObject(null, obj, map);
		return map;
	}

	/**
	 * 遍历对象</br>
	 * 生成一个Map:{"tags":["utou"],"toContacts.contactType":["to"],"toContacts.contact":["689","688"],"option.channel":["notice"]}
	 * 
	 * @param obj
	 * @param relations
	 */
	public static void foreachObject(Object obj, SetHashMap<String, Object> relations) {
		foreachObject(null, obj, relations);
	}

	public static void foreachObject(String parentObj, Object obj, SetHashMap<String, Object> relations) {
		JSONObject jb = (JSONObject) JSONObject.toJSON(obj);
		for (String p : jb.keySet()) {
			Object v = jb.get(p);
			if (v == null)
				continue;
			if (parentObj != null)
				p = parentObj + "." + p;
			if (isSingleType(v)) {
				relations.put(p, v);
			} else if (v instanceof JSONArray) {
				JSONArray ja = (JSONArray) v;
				for (Object o : ja) {
					// 基本类型数组
					if (isSingleType(o)) {
						relations.put(p, v);
						break;
					} else {
						foreachObject(p, o, relations);
					}
				}
			} else {
				foreachObject(p, v, relations);
			}
		}
	}

	public static boolean isSingleType(Object obj) {
		if (obj instanceof String)
			return true;
		if (obj instanceof Date)
			return true;
		if (obj instanceof java.sql.Date)
			return true;
		if (ClassUtils.isPrimitiveOrWrapper(obj.getClass()))
			return true;
		if (ClassUtils.isPrimitiveArray(obj.getClass()))
			return true;
		if (ClassUtils.isPrimitiveWrapperArray(obj.getClass()))
			return true;
		return false;
	}

	public static boolean isLoopType(Object obj) {
		if (obj instanceof List || obj instanceof Collection || obj instanceof Set || obj instanceof Iterable) {
			return true;
		}
		return false;
	}

	/**
	 * 打印这个对象的默认字段,只针对基础字段
	 * 
	 * @param object
	 */
	public static void printObjectDefField(Object object) {
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(object.getClass());
		for (int i = 0; i < methods.length; i++) {
			Method m = methods[i];
			String n = m.getName();
			try {
				Class<?>[] pp = m.getParameterTypes();
				if (pp.length != 1)
					continue;
				Class<?> p = pp[0];
				if (!ClassUtils.isPrimitiveOrWrapper(p))
					continue;
				if (StringUtils.left(n, 3).equals("set")) {
					try {
						Object value = MethodUtils.invokeMethod(object, "g" + StringUtils.substring(n, 1), null);
						System.out.println(
							"private " + p.getSimpleName() + " " + StringUtils.substring(n, 3, 4).toLowerCase() + StringUtils.substring(n, 4) + " = " + value.toString() + ";");
					} catch (Exception e) {
						try {
							Object value = MethodUtils.invokeMethod(object, "is" + StringUtils.substring(n, 3), null);
							System.out.println(
								"private " + p.getSimpleName() + " " + StringUtils.substring(n, 3, 4).toLowerCase() + StringUtils.substring(n, 4) + " = " + value.toString() + ";");
						} catch (Exception e1) {
							System.out.println("private " + p.getSimpleName() + " " + StringUtils.substring(n, 3, 4).toLowerCase() + StringUtils.substring(n, 4) + ";");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	public static void main(String[] args) {
		// List<String> link = Lists.newLinkedList();
		// List<String> array = Lists.newArrayList("1", "2");
		// List<String> array2 = Lists.newArrayList("3", "4");
		// System.out.println(softCast("12312", array));
	}
}
