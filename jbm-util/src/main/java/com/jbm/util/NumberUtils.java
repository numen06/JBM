package com.jbm.util;

import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.NumberFormat;

/**
 * 数字处理类
 *
 * @author Wesley
 */
public class NumberUtils extends cn.hutool.core.util.NumberUtil {

    /**
     * 格式化数字称字符串(-1代表不格式化)
     *
     * @param number         数字类型
     * @param integerDigits  整数位
     * @param fractionDigits 小数位
     * @return 格式化之后的字符串
     */
    public static String format(Object number, int integerDigits, int fractionDigits) {
        return format(number, integerDigits, fractionDigits, null);
    }

    /**
     * 格式化数字称字符串(-1代表不格式化)
     *
     * @param number         数字类型
     * @param integerDigits  整数位
     * @param fractionDigits 小数位
     * @param roundingMode   四舍五入的模式
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
     * @param number  数字类型
     * @param pattern 格式化字符串
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
            result = org.apache.commons.lang.math.NumberUtils.max(ArrayUtils.toPrimitive((Long[]) values, Long.MIN_VALUE));
        else if (values instanceof Integer[])
            result = org.apache.commons.lang.math.NumberUtils.max(ArrayUtils.toPrimitive((Integer[]) values, Integer.MIN_VALUE));
        else if (values instanceof Short[])
            result = org.apache.commons.lang.math.NumberUtils.max(ArrayUtils.toPrimitive((Short[]) values, Short.MIN_VALUE));
        else if (values instanceof Float[])
            result = org.apache.commons.lang.math.NumberUtils.max(ArrayUtils.toPrimitive((Float[]) values, Float.MIN_VALUE));
        else if (values instanceof Byte[])
            result = org.apache.commons.lang.math.NumberUtils.max(ArrayUtils.toPrimitive((Byte[]) values, Byte.MIN_VALUE));
        else if (values instanceof Double[])
            result = org.apache.commons.lang.math.NumberUtils.max(ArrayUtils.toPrimitive((Double[]) values, Double.MIN_VALUE));
        return (T) result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T minimum(T... values) {
        Object result = null;
        if (values instanceof Long[])
            result = org.apache.commons.lang.math.NumberUtils.min(ArrayUtils.toPrimitive((Long[]) values, Long.MAX_VALUE));
        else if (values instanceof Integer[])
            result = org.apache.commons.lang.math.NumberUtils.min(ArrayUtils.toPrimitive((Integer[]) values, Integer.MAX_VALUE));
        else if (values instanceof Short[])
            result = org.apache.commons.lang.math.NumberUtils.min(ArrayUtils.toPrimitive((Short[]) values, Short.MAX_VALUE));
        else if (values instanceof Float[])
            result = org.apache.commons.lang.math.NumberUtils.min(ArrayUtils.toPrimitive((Float[]) values, Float.MAX_VALUE));
        else if (values instanceof Byte[])
            result = org.apache.commons.lang.math.NumberUtils.min(ArrayUtils.toPrimitive((Byte[]) values, Byte.MAX_VALUE));
        else if (values instanceof Double[])
            result = org.apache.commons.lang.math.NumberUtils.min(ArrayUtils.toPrimitive((Double[]) values, Double.MAX_VALUE));
        return (T) result;
    }




}
