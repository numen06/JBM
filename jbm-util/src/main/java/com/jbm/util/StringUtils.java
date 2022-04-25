package com.jbm.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;

/**
 * 
 * 处理字符串的工具类
 * 
 * @author Wesley
 * 
 */
public class StringUtils extends org.apache.commons.lang.StringUtils {

	public final static String COMMA = ",";

	public final static String SEMICOLON = ";";

	public final static String DOT = ".";

	public final static String bond(String... strs) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strs.length; i++)
			sb.append(strs[i]);
		return sb.toString();
	}

	public static String join(String separator, Object... array) {
		return StringUtils.join(array, separator);
	}

	public static boolean isTrimedEmpty(String s) {
		return s == null || StringUtils.trimToEmpty(s).length() == 0;
	}

	/**
	 * <p>
	 * Splits the provided text into an array, using whitespace as the
	 * separator. Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * </p>
	 * 
	 * <p>
	 * The separator is not included in the returned String array. Adjacent
	 * separators are treated as one separator. For more control over the split
	 * use the StrTokenizer class.
	 * </p>
	 * 
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.split(null)       = null
	 * StringUtils.split("")         = []
	 * StringUtils.split("abc def")  = ["abc", "def"]
	 * StringUtils.split("abc  def") = ["abc", "def"]
	 * StringUtils.split(" abc ")    = ["abc"]
	 * </pre>
	 * 
	 * @param str
	 *            the String to parse, may be null
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
	 */
	public static String[] splitToEmpty(String str) {
		return ArrayUtils.nullToEmpty(org.apache.commons.lang.StringUtils.split(str, COMMA));
	}

	public static List<String> splitToEmptyList(String str) {
		String[] arr = splitToEmpty(str);
		return ListUtils.newArrayList(arr);
	}

	public static boolean isAllBlank(String... strings) {
		for (int i = 0; i < strings.length; i++) {
			String str = strings[i];
			if (org.apache.commons.lang.StringUtils.isNotBlank(str))
				return false;
		}
		return true;
	}

	public static boolean isAllNotBlank(String... strings) {
		for (int i = 0; i < strings.length; i++) {
			String str = strings[i];
			if (org.apache.commons.lang.StringUtils.isBlank(str))
				return false;
		}
		return true;
	}

	/**
	 * <p>
	 * Splits the provided text into an array, separator specified. This is an
	 * alternative to using StringTokenizer.
	 * </p>
	 * 
	 * <p>
	 * The separator is not included in the returned String array. Adjacent
	 * separators are treated as one separator. For more control over the split
	 * use the StrTokenizer class.
	 * </p>
	 * 
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.split(null, *)         = null
	 * StringUtils.split("", *)           = []
	 * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
	 * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
	 * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
	 * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
	 * </pre>
	 * 
	 * @param str
	 *            the String to parse, may be null
	 * @param separatorChar
	 *            the character used as the delimiter
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
	 * @since 2.0
	 */
	public static String[] splitToEmpty(String str, String separatorChar) {
		return ArrayUtils.nullToEmpty(org.apache.commons.lang.StringUtils.split(str, separatorChar));
	}

	/**
	 * 将字符串换成对应类型的值
	 * 
	 * @param source
	 *            来源字符串
	 * @param defaultValue
	 *            转换未成功的默认值
	 * @return
	 */
	public static <T extends Object> T exchangeType(String source, T defaultValue) {
		return exchangeType(source, defaultValue.getClass(), defaultValue);
	}

	/**
	 * 将字符串换成对应类型的值
	 * 
	 * @param s
	 *            来源字符串
	 * @param clazz
	 *            目的类型
	 * @return
	 */
	public static <T extends Object> T exchangeType(String s, Class<T> clazz) {
		return exchangeType(s, clazz, null);
	}

	/**
	 * 将字符串换成对应类型的值
	 * 
	 * @param s
	 *            来源字符串
	 * @param clazz
	 *            目的类型
	 * @param defaultValue
	 *            转换未成功的默认值
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T exchangeType(String s, Class<? extends Object> clazz, T defaultValue) {
		Object o = null;
		try {
			if (clazz == Integer.class) {
				o = Integer.parseInt(s);
			} else if (clazz == Double.class) {
				o = Double.parseDouble(s);
			} else if (clazz == Long.class) {
				o = Long.parseLong(s);
			} else if (clazz == Short.class) {
				o = Short.parseShort(s);
			} else if (clazz == Float.class) {
				o = Float.parseFloat(s);
			} else if (clazz == Character.class) {
				o = s.charAt(0);
			} else if (clazz == Boolean.class) {
				o = Boolean.valueOf(s);
			} else if (clazz == Date.class) {
				o = TimeUtils.parseDate(s);
			} else if (clazz == java.sql.Date.class) {
				Date date = TimeUtils.parseDate(s);
				o = new java.sql.Date(date.getTime());
			}
		} catch (Exception e) {
			return defaultValue;
		}
		return (T) o;
	}

	/**
	 * Check that the given CharSequence is neither <code>null</code> nor of
	 * length 0. Note: Will return <code>true</code> for a CharSequence that
	 * purely consists of whitespace.
	 * <p>
	 * 
	 * <pre>
	 * StringUtils.hasLength(null) = false
	 * StringUtils.hasLength("") = false
	 * StringUtils.hasLength(" ") = true
	 * StringUtils.hasLength("Hello") = true
	 * </pre>
	 * 
	 * @param str
	 *            the CharSequence to check (may be <code>null</code>)
	 * @return <code>true</code> if the CharSequence is not null and has length
	 * @see #hasText(String)
	 */
	public static boolean hasLength(CharSequence str) {
		return (str != null && str.length() > 0);
	}

	/**
	 * Check that the given String is neither <code>null</code> nor of length 0.
	 * Note: Will return <code>true</code> for a String that purely consists of
	 * whitespace.
	 * 
	 * @param str
	 *            the String to check (may be <code>null</code>)
	 * @return <code>true</code> if the String is not null and has length
	 * @see #hasLength(CharSequence)
	 */
	public static boolean hasLength(String str) {
		return hasLength((CharSequence) str);
	}

	/**
	 * Check whether the given CharSequence has actual text. More specifically,
	 * returns <code>true</code> if the string not <code>null</code>, its length
	 * is greater than 0, and it contains at least one non-whitespace character.
	 * <p>
	 * 
	 * <pre>
	 * StringUtils.hasText(null) = false
	 * StringUtils.hasText("") = false
	 * StringUtils.hasText(" ") = false
	 * StringUtils.hasText("12345") = true
	 * StringUtils.hasText(" 12345 ") = true
	 * </pre>
	 * 
	 * @param str
	 *            the CharSequence to check (may be <code>null</code>)
	 * @return <code>true</code> if the CharSequence is not <code>null</code>,
	 *         its length is greater than 0, and it does not contain whitespace
	 *         only
	 * @see java.lang.Character#isWhitespace
	 */
	public static boolean hasText(CharSequence str) {
		if (!hasLength(str)) {
			return false;
		}
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given String has actual text. More specifically,
	 * returns <code>true</code> if the string not <code>null</code>, its length
	 * is greater than 0, and it contains at least one non-whitespace character.
	 * 
	 * @param str
	 *            the String to check (may be <code>null</code>)
	 * @return <code>true</code> if the String is not <code>null</code>, its
	 *         length is greater than 0, and it does not contain whitespace only
	 * @see #hasText(CharSequence)
	 */
	public static boolean hasText(String str) {
		return hasText((CharSequence) str);
	}

	/**
	 * 将Bean中的字符串去除所有的前后空格并且置为NULL
	 * 
	 * @param bean
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public static <T> T beanTrimToNull(T bean) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(bean);
		for (int i = 0; i < descriptors.length; i++) {
			String name = descriptors[i].getName();
			if (descriptors[i].getReadMethod() != null && descriptors[i].getWriteMethod() != null) {
				Object obj = PropertyUtils.getProperty(bean, name);
				if (obj != null && obj instanceof String) {
					PropertyUtils.setProperty(bean, name, StringUtils.trimToNull((String) obj));
				}
			}
		}
		return bean;
	}

	public static <T> List<T> splitToList(String text, Class<T> clazz) {
		List<T> list = new ArrayList<T>();
		try {
			list = Lists.newArrayList(JSON.parseArray(JSON.toJSONString(StringUtils.splitToEmpty(text)), clazz));
		} catch (Exception e) {
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] splitToArray(String text, Class<T> clazz) {
		List<T> list = splitToList(text, clazz);
		return (T[]) list.toArray();
	}

	public static String replace(String inString, String oldPattern, String newPattern) {
		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
			return inString;
		}
		StringBuilder sb = new StringBuilder();
		int pos = 0; // our position in the old string
		int index = inString.indexOf(oldPattern);
		// the index of an occurrence we've found, or -1
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString.substring(pos, index));
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}
		sb.append(inString.substring(pos));
		// remember to append any characters to the right of a match
		return sb.toString();
	}

	public static String[] delimitedListToStringArray(String str, String delimiter) {
		return delimitedListToStringArray(str, delimiter, null);
	}

	public static String[] delimitedListToStringArray(String str, String delimiter, String charsToDelete) {
		if (str == null) {
			return new String[0];
		}
		if (delimiter == null) {
			return new String[] { str };
		}
		List<String> result = new ArrayList<String>();
		if ("".equals(delimiter)) {
			for (int i = 0; i < str.length(); i++) {
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		} else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {
				// Add rest of String, but not in case of empty input.
				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		return toStringArray(result);
	}

	/**
	 * 转换成字符串数组
	 * 
	 * @param collection
	 * @return
	 */
	public static String[] toStringArray(Collection<String> collection) {
		if (collection == null) {
			return null;
		}
		return collection.toArray(new String[collection.size()]);
	}

	/**
	 * 间隔输出字符串
	 * 
	 * @param coll
	 *            循环
	 * @param delim
	 *            间隔
	 * @return
	 */
	public static String collectionToDelimitedString(Collection<?> coll, String delim) {
		return collectionToDelimitedString(coll, delim, "", "");
	}

	/**
	 * 
	 * @param coll
	 *            循环
	 * @param delim
	 *            间隔
	 * @param prefix
	 *            前缀
	 * @param suffix
	 *            后缀
	 * @return
	 */
	public static String collectionToDelimitedString(Collection<?> coll, String delim, String prefix, String suffix) {
		if (CollectionUtils.isEmpty(coll)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append(prefix).append(it.next()).append(suffix);
			if (it.hasNext()) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

	/**
	 * 删除一个字符
	 * 
	 * @param inString
	 * @param charsToDelete
	 * @return
	 */
	public static String deleteAny(String inString, String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			return inString;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String toString(Collection<?> coll) {
		return collectionToDelimitedString(coll, ",", "", "");
	}

	public static String toString(Object[] arr) {
		return join(arr, ",");
	}

	@SuppressWarnings("rawtypes")
	public static String toString(Map arr) {
		return arr.toString();
	}

	public static String nullToEmpty(Object obj) {
		if (obj == null)
			return "";
		return obj.toString();
	}

	public static boolean contains(String str, String... searchStrs) {
		for (int i = 0; i < searchStrs.length; i++) {
			if (!StringUtils.contains(str, searchStrs[i]))
				return false;
		}
		return true;
	}


}
