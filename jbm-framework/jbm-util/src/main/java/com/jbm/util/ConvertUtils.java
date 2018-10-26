package com.jbm.util;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.CalendarConverter;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.SqlDateConverter;
import org.apache.commons.beanutils.converters.SqlTimeConverter;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;

import com.jbm.util.support.StandConvertUtilsBean;

/**
 * 
 * 转换器
 * 
 * @author Wesley
 * 
 */
@SuppressWarnings("rawtypes")
public class ConvertUtils extends org.apache.commons.beanutils.ConvertUtils {

	private static boolean isRegisterDate = false;

	static {
		registerDate(true);
	}

	/**
	 * 注册相应的Date转换类
	 * 
	 * @param throwException
	 */
	public final static void registerDate(boolean throwException) {
		if (isRegisterDate)
			return;
		Map<Class, Converter> map = timeConverters(throwException);
		for (Class key : map.keySet()) {
			Converter con = map.get(key);

			ConvertUtils.register(con, key);
			StandConvertUtilsBean.getInstance().register(con, key);
		}
		isRegisterDate = true;
	}

	protected static Map<Class, Converter> timeConverters(boolean throwException) {
		Map<Class, Converter> converters = new HashMap<Class, Converter>();
		DateConverter dateConverter = throwException ? new DateConverter() : new DateConverter(null);
		dateConverter.setPatterns(TimeUtil.parsePatterns);
		CalendarConverter calendarConverter = throwException ? new CalendarConverter() : new CalendarConverter(null);
		calendarConverter.setPatterns(TimeUtil.parsePatterns);
		SqlDateConverter sqlDateConverter = throwException ? new SqlDateConverter() : new SqlDateConverter(null);
		sqlDateConverter.setPatterns(TimeUtil.parsePatterns);
		SqlTimeConverter sqlTimeConverter = throwException ? new SqlTimeConverter() : new SqlTimeConverter(null);
		sqlTimeConverter.setPatterns(TimeUtil.parsePatterns);
		SqlTimestampConverter sqlTimestampConverter = throwException ? new SqlTimestampConverter() : new SqlTimestampConverter(null);
		sqlTimestampConverter.setPatterns(TimeUtil.parsePatterns);

		converters.put(java.util.Date.class, dateConverter);
		converters.put(Calendar.class, calendarConverter);
		converters.put(java.sql.Date.class, sqlDateConverter);
		converters.put(java.sql.Time.class, sqlTimeConverter);
		converters.put(Timestamp.class, sqlTimestampConverter);
		return converters;
	}

	/**
	 * <p>
	 * Convert the specified value into a String.
	 * </p>
	 * 
	 * <p>
	 * For more details see <code>ConvertUtilsBean</code>.
	 * </p>
	 * 
	 * @param value
	 *            Value to be converted (may be null)
	 * @return The converted String value
	 * 
	 * @see ConvertUtilsBean#convert(Object)
	 */
	public static String convert(Object value) {
		return org.apache.commons.beanutils.ConvertUtils.convert(value);
	}

	/**
	 * <p>
	 * Convert the specified value to an object of the specified class (if
	 * possible). Otherwise, return a String representation of the value.
	 * </p>
	 * 
	 * <p>
	 * For more details see <code>ConvertUtilsBean</code>.
	 * </p>
	 * 
	 * @param value
	 *            Value to be converted (may be null)
	 * @param clazz
	 *            Java class to be converted to
	 * @return The converted value
	 * 
	 * @see ConvertUtilsBean#convert(String, Class)
	 */
	public static Object convert(String value, Class clazz) {
		return org.apache.commons.beanutils.ConvertUtils.convert(value, clazz);

	}

	@SuppressWarnings("unchecked")
	public static <T> T converts(String value, Class<T> clazz) {
		return (T) org.apache.commons.beanutils.ConvertUtils.convert(value, clazz);
	}

	/**
	 * <p>
	 * Convert an array of specified values to an array of objects of the
	 * specified class (if possible).
	 * </p>
	 * 
	 * <p>
	 * For more details see <code>ConvertUtilsBean</code>.
	 * </p>
	 * 
	 * @param values
	 *            Array of values to be converted
	 * @param clazz
	 *            Java array or element class to be converted to
	 * @return The converted value
	 * 
	 * @see ConvertUtilsBean#convert(String[], Class)
	 */
	public static Object convert(String[] values, Class clazz) {
		return org.apache.commons.beanutils.ConvertUtils.convert(values, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> T converts(String[] value, Class<T> clazz) {
		return (T) org.apache.commons.beanutils.ConvertUtils.convert(value, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> T converts(String[] value, Class<T> clazz, T reserve) {
		try {
			return (T) StandConvertUtilsBean.getInstance().convert(value, clazz);
		} catch (ConversionException e) {
			return reserve;
		}
	}

	/**
	 * <p>
	 * Convert the value to an object of the specified class (if possible).
	 * </p>
	 * 
	 * @param value
	 *            Value to be converted (may be null)
	 * @param targetType
	 *            Class of the value to be converted to
	 * @return The converted value
	 * 
	 * @exception ConversionException
	 *                if thrown by an underlying Converter
	 */
	public static Object convert(Object value, Class targetType) {
		return org.apache.commons.beanutils.ConvertUtils.convert(value, targetType);
	}

	@SuppressWarnings("unchecked")
	public static <T> T converts(Object value, Class<T> clazz) {
		return (T) org.apache.commons.beanutils.ConvertUtils.convert(value, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> T converts(Object value, Class<T> clazz, T reserve) {
		try {
			return (T) StandConvertUtilsBean.getInstance().convert(value, clazz);
		} catch (ConversionException e) {
			return reserve;
		}
	}

	public static void main(String[] args) {
		System.out.println(ConvertUtils.converts("2013-01-01", Date.class, TimeUtil.now()));
	}

}
