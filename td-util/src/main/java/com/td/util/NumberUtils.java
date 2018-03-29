package com.td.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.NumberFormat;

/**
 * 数字处理类
 * 
 * @author Wesley
 * 
 */
public class NumberUtils extends org.apache.commons.lang.math.NumberUtils {

	/**
	 * 格式化数字称字符串(-1代表不格式化)
	 * 
	 * @param number
	 *            数字类型
	 * @param integerDigits
	 *            整数位
	 * @param fractionDigits
	 *            小数位
	 * @return 格式化之后的字符串
	 */
	public static String format(Object number, int integerDigits, int fractionDigits) {
		return format(number, integerDigits, fractionDigits, null);
	}

	/**
	 * 格式化数字称字符串(-1代表不格式化)
	 * 
	 * @param number
	 *            数字类型
	 * @param integerDigits
	 *            整数位
	 * @param fractionDigits
	 *            小数位
	 * @param roundingMode
	 *            四舍五入的模式
	 * @return 格式化之后的字符串
	 */
	public static String format(Object number, int integerDigits, int fractionDigits, RoundingMode roundingMode) {
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setGroupingUsed(false);
		if (roundingMode != null)
			nf.setRoundingMode(roundingMode);
		if (integerDigits >= 0) {
			nf.setMaximumIntegerDigits(integerDigits);
			nf.setMinimumIntegerDigits(integerDigits);
		}
		if (fractionDigits >= 0) {
			nf.setMinimumFractionDigits(fractionDigits);
			nf.setMaximumFractionDigits(fractionDigits);
		}
		return nf.format(number);
	}

	/**
	 * 格式化
	 * 
	 * @param number
	 *            数字类型
	 * @param pattern
	 *            格式化字符串
	 * @return 格式化之后的字符串
	 */
	public static String format(Object number, String pattern) {
		// DecimalFormat df = new DecimalFormat(pattern);
		return MessageFormat.format("{0,number," + pattern + "}", number);
	}

	/**
	 * 将数字颠倒
	 * 
	 * @param value
	 * @return
	 */
	public static Long reverse(Long value) {
		String data = new StringBuffer(value.toString()).reverse().toString();
		Long reverse = Long.parseLong(data);
		return reverse;
	}

	@SuppressWarnings("unchecked")
	public static <T> T maximum(T... values) {
		Object result = null;
		if (values instanceof Long[])
			result = NumberUtils.max(ArrayUtils.toPrimitive((Long[]) values, Long.MIN_VALUE));
		else if (values instanceof Integer[])
			result = NumberUtils.max(ArrayUtils.toPrimitive((Integer[]) values, Integer.MIN_VALUE));
		else if (values instanceof Short[])
			result = NumberUtils.max(ArrayUtils.toPrimitive((Short[]) values, Short.MIN_VALUE));
		else if (values instanceof Float[])
			result = NumberUtils.max(ArrayUtils.toPrimitive((Float[]) values, Float.MIN_VALUE));
		else if (values instanceof Byte[])
			result = NumberUtils.max(ArrayUtils.toPrimitive((Byte[]) values, Byte.MIN_VALUE));
		else if (values instanceof Double[])
			result = NumberUtils.max(ArrayUtils.toPrimitive((Double[]) values, Double.MIN_VALUE));
		return (T) result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T minimum(T... values) {
		Object result = null;
		if (values instanceof Long[])
			result = NumberUtils.min(ArrayUtils.toPrimitive((Long[]) values, Long.MAX_VALUE));
		else if (values instanceof Integer[])
			result = NumberUtils.min(ArrayUtils.toPrimitive((Integer[]) values, Integer.MAX_VALUE));
		else if (values instanceof Short[])
			result = NumberUtils.min(ArrayUtils.toPrimitive((Short[]) values, Short.MAX_VALUE));
		else if (values instanceof Float[])
			result = NumberUtils.min(ArrayUtils.toPrimitive((Float[]) values, Float.MAX_VALUE));
		else if (values instanceof Byte[])
			result = NumberUtils.min(ArrayUtils.toPrimitive((Byte[]) values, Byte.MAX_VALUE));
		else if (values instanceof Double[])
			result = NumberUtils.min(ArrayUtils.toPrimitive((Double[]) values, Double.MAX_VALUE));
		return (T) result;
	}

	/**
	 * 对double数据进行取精度.
	 * 
	 * @param value
	 *            double数据.
	 * @param scale
	 *            精度位数(保留的小数位数).
	 * @param roundingMode
	 *            精度取值方式.
	 * @return 精度计算后的数据.
	 */
	public static double round(double value, int scale, RoundingMode roundingMode) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(scale, roundingMode);
		double d = bd.doubleValue();
		bd = null;
		return d;
	}

	/**
	 * 
	 * 对double数据进行取精度,默认四舍五入
	 * 
	 * @param value
	 *            double数据.
	 * @param scale
	 *            精度位数(保留的小数位数).
	 * @return
	 */
	public static double round(double value, int scale) {
		return round(value, scale, RoundingMode.HALF_UP);
	}

	public static void main(String[] args) {
		System.out.println(NumberUtils.minimum(123123.1, 1d, 3.12312));
		System.out.println(NumberUtils.format(100000, "00.000"));
	}

}
