package com.jbm.util;

/**
 * 处理布尔类型的工具类
 *
 * @author Wesley
 */
public class BooleanUtils extends org.apache.commons.lang.BooleanUtils {

    /**
     * <p>
     * Converts an Integer to a Boolean using the convention that
     * <code>zero</code> is <code>false</code>.
     * </p>
     *
     * <p>
     * <code>null</code> will be converted to <code>null</code>.
     * </p>
     *
     * <pre>
     *   BooleanUtils.toBoolean(new Integer(0))    = Boolean.FALSE
     *   BooleanUtils.toBoolean(new Integer(1))    = Boolean.TRUE
     *   BooleanUtils.toBoolean(new Integer(null)) = Boolean.FALSE
     * </pre>
     *
     * @param value the Integer to convert
     * @return Boolean.TRUE if non-zero, Boolean.FALSE if zero,
     * <code>null</code> if <code>null</code> input
     */
    public static Boolean softToBooleanObject(Integer value) {
        return BooleanUtils.toBooleanObject(value == null ? new Integer(0) : value);
    }

    /**
     * <p>
     * Converts an Integer to a Boolean using the convention that
     * <code>zero</code> is <code>false</code>.
     * </p>
     *
     * <p>
     * <code>null</code> will be converted to <code>null</code>.
     * </p>
     *
     * <pre>
     *   BooleanUtils.toBoolean(new Integer(0))    = Boolean.FALSE
     *   BooleanUtils.toBoolean(new Integer(1))    = Boolean.TRUE
     *   BooleanUtils.toBoolean(new Integer(null)) = Boolean.FALSE
     * </pre>
     *
     * @param value the Integer to convert
     * @return Boolean.TRUE if non-zero, Boolean.FALSE if zero,
     * <code>null</code> if <code>null</code> input
     */
    public static boolean softToBoolean(Integer value) {
        return BooleanUtils.toBoolean(value == null ? new Integer(0) : value);
    }
}
