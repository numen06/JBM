package com.jbm.util.support;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.*;
import org.apache.commons.collections.FastHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;

@SuppressWarnings("rawtypes")
public class StandConvertUtilsBean {

    private static final Integer ZERO = new Integer(0);
    private static final Character SPACE = new Character(' ');
    protected static StandConvertUtilsBean convertUtilsBean;
    private boolean throwException = true;
    private boolean defaultNull = true;
    private int defaultArraySize = 0;
    /**
     * The set of {@link Converter}s that can be used to convert Strings into
     * objects of a specified Class, keyed by the destination Class.
     */
    private FastHashMap converters = new FastHashMap();
    /**
     * The <code>Log</code> instance for this class.
     */
    private Log log = LogFactory.getLog(ConvertUtils.class);

    public StandConvertUtilsBean(boolean throwException, boolean defaultNull, int defaultArraySize) {
        super();
        this.throwException = throwException;
        this.defaultNull = defaultNull;
        this.defaultArraySize = defaultArraySize;
    }

    /**
     * Construct a bean with standard converters registered
     */
    public StandConvertUtilsBean() {
        converters.setFast(false);
        deregister();
        converters.setFast(true);
    }

    /**
     * Get singleton instance
     *
     * @return The singleton instance
     */
    public static StandConvertUtilsBean getInstance() {
        convertUtilsBean = convertUtilsBean != null ? convertUtilsBean : new StandConvertUtilsBean();
        return convertUtilsBean;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    // ------------------------------------------------------- Class Methods

    public boolean isDefaultNull() {
        return defaultNull;
    }

    // ------------------------------------------------------- Variables

    public void setDefaultNull(boolean defaultNull) {
        this.defaultNull = defaultNull;
    }

    public int getDefaultArraySize() {
        return defaultArraySize;
    }

    // ------------------------------------------------------- Constructors

    public void setDefaultArraySize(int defaultArraySize) {
        this.defaultArraySize = defaultArraySize;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Convert the specified value into a String. If the specified value is an
     * array, the first element (converted to a String) will be returned. The
     * registered {@link Converter} for the <code>java.lang.String</code> class
     * will be used, which allows applications to customize Object->String
     * conversions (the default implementation simply uses toString()).
     *
     * @param value Value to be converted (may be null)
     * @return The converted String value
     */
    public String convert(Object value) {

        if (value == null) {
            return ((String) null);
        } else if (value.getClass().isArray()) {
            if (Array.getLength(value) < 1) {
                return (null);
            }
            value = Array.get(value, 0);
            if (value == null) {
                return ((String) null);
            } else {
                Converter converter = lookup(String.class);
                return ((String) converter.convert(String.class, value));
            }
        } else {
            Converter converter = lookup(String.class);
            return ((String) converter.convert(String.class, value));
        }

    }

    /**
     * Convert the specified value to an object of the specified class (if
     * possible). Otherwise, return a String representation of the value.
     *
     * @param value Value to be converted (may be null)
     * @param clazz Java class to be converted to
     * @return The converted value
     * @throws ConversionException if thrown by an underlying Converter
     */

    @SuppressWarnings("unchecked")
    public Object convert(String value, Class clazz) {

        if (log.isDebugEnabled()) {
            log.debug("Convert string '" + value + "' to class '" + clazz.getName() + "'");
        }
        Converter converter = lookup(clazz);
        if (converter == null) {
            converter = lookup(String.class);
        }
        if (log.isTraceEnabled()) {
            log.trace("  Using converter " + converter);
        }
        return (converter.convert(clazz, value));

    }

    /**
     * Convert an array of specified values to an array of objects of the
     * specified class (if possible). If the specified Java class is itself an
     * array class, this class will be the type of the returned value.
     * Otherwise, an array will be constructed whose component type is the
     * specified class.
     *
     * @param values Array of values to be converted
     * @param clazz  Java array or element class to be converted to
     * @return The converted value
     * @throws ConversionException if thrown by an underlying Converter
     */
    @SuppressWarnings("unchecked")
    public Object convert(String[] values, Class clazz) {

        Class type = clazz;
        if (clazz.isArray()) {
            type = clazz.getComponentType();
        }
        if (log.isDebugEnabled()) {
            log.debug("Convert String[" + values.length + "] to class '" + type.getName() + "[]'");
        }
        Converter converter = lookup(type);
        if (converter == null) {
            converter = lookup(String.class);
        }
        if (log.isTraceEnabled()) {
            log.trace("  Using converter " + converter);
        }
        Object array = Array.newInstance(type, values.length);
        for (int i = 0; i < values.length; i++) {
            Array.set(array, i, converter.convert(type, values[i]));
        }
        return (array);

    }

    /**
     * <p>
     * Convert the value to an object of the specified class (if possible).
     * </p>
     *
     * @param value      Value to be converted (may be null)
     * @param targetType Class of the value to be converted to
     * @return The converted value
     * @throws ConversionException if thrown by an underlying Converter
     */
    @SuppressWarnings("unchecked")
    public Object convert(Object value, Class targetType) {

        Class sourceType = value == null ? null : value.getClass();

        if (log.isDebugEnabled()) {
            if (value == null) {
                log.debug("Convert null value to type '" + targetType.getName() + "'");
            } else {
                log.debug("Convert type '" + sourceType.getName() + "' value '" + value + "' to type '" + targetType.getName() + "'");
            }
        }

        Object converted = value;
        Converter converter = lookup(sourceType, targetType);
        if (converter != null) {
            if (log.isTraceEnabled()) {
                log.trace("  Using converter " + converter);
            }
            converted = converter.convert(targetType, value);
        }
        if (targetType == String.class && converted != null && !(converted instanceof String)) {

            // NOTE: For backwards compatibility, if the Converter
            // doesn't handle conversion-->String then
            // use the registered String Converter
            converter = lookup(String.class);
            if (converter != null) {
                if (log.isTraceEnabled()) {
                    log.trace("  Using converter " + converter);
                }
                converted = converter.convert(String.class, converted);
            }

            // If the object still isn't a String, use toString() method
            if (converted != null && !(converted instanceof String)) {
                converted = converted.toString();
            }

        }
        return converted;

    }

    /**
     * Remove all registered {@link Converter}s, and re-establish the standard
     * Converters.
     */
    public void deregister() {

        converters.clear();

        registerPrimitives(this.throwException);
        registerStandard(this.throwException, this.defaultNull);
        registerOther(this.throwException);
        registerArrays(this.throwException, 0);
        register(BigDecimal.class, new BigDecimalConverter());
        register(BigInteger.class, new BigIntegerConverter());
    }

    /**
     * Register the provided converters with the specified defaults.
     *
     * @param throwException   <code>true</code> if the converters should throw an exception
     *                         when a conversion error occurs, otherwise <code>
     *                         <code>false</code> if a default value should be used.
     * @param defaultNull      <code>true</code>if the <i>standard</i> converters (see
     *                         {@link StandConvertUtilsBean#registerStandard(boolean, boolean)}
     *                         ) should use a default value of <code>null</code>, otherwise
     *                         <code>false</code>. N.B. This values is ignored if
     *                         <code>throwException</code> is <code>true</code>
     * @param defaultArraySize The size of the default array value for array converters (N.B.
     *                         This values is ignored if <code>throwException</code> is
     *                         <code>true</code>). Specifying a value less than zero causes a
     *                         <code>null<code> value to be used for
     *                         the default.
     */
    public void register(boolean throwException, boolean defaultNull, int defaultArraySize) {
        registerPrimitives(throwException);
        registerStandard(throwException, defaultNull);
        registerOther(throwException);
        registerArrays(throwException, defaultArraySize);
    }

    /**
     * Register the converters for primitive types. </p> This method registers
     * the following converters:
     * <ul>
     * <li><code>Boolean.TYPE</code> - {@link BooleanConverter}</li>
     * <li><code>Byte.TYPE</code> - {@link ByteConverter}</li>
     * <li><code>Character.TYPE</code> - {@link CharacterConverter}</li>
     * <li><code>Double.TYPE</code> - {@link DoubleConverter}</li>
     * <li><code>Float.TYPE</code> - {@link FloatConverter}</li>
     * <li><code>Integer.TYPE</code> - {@link IntegerConverter}</li>
     * <li><code>Long.TYPE</code> - {@link LongConverter}</li>
     * <li><code>Short.TYPE</code> - {@link ShortConverter}</li>
     * </ul>
     *
     * @param throwException <code>true</code> if the converters should throw an exception
     *                       when a conversion error occurs, otherwise <code>
     *                       <code>false</code> if a default value should be used.
     */
    private void registerPrimitives(boolean throwException) {
        register(Boolean.TYPE, throwException ? new BooleanConverter() : new BooleanConverter(Boolean.FALSE));
        register(Byte.TYPE, throwException ? new ByteConverter() : new ByteConverter(ZERO));
        register(Character.TYPE, throwException ? new CharacterConverter() : new CharacterConverter(SPACE));
        register(Double.TYPE, throwException ? new DoubleConverter() : new DoubleConverter(ZERO));
        register(Float.TYPE, throwException ? new FloatConverter() : new FloatConverter(ZERO));
        register(Integer.TYPE, throwException ? new IntegerConverter() : new IntegerConverter(ZERO));
        register(Long.TYPE, throwException ? new LongConverter() : new LongConverter(ZERO));
        register(Short.TYPE, throwException ? new ShortConverter() : new ShortConverter(ZERO));
    }

    /**
     * Register the converters for standard types. </p> This method registers
     * the following converters:
     * <ul>
     * <li><code>BigDecimal.class</code> - {@link BigDecimalConverter}</li>
     * <li><code>BigInteger.class</code> - {@link BigIntegerConverter}</li>
     * <li><code>Boolean.class</code> - {@link BooleanConverter}</li>
     * <li><code>Byte.class</code> - {@link ByteConverter}</li>
     * <li><code>Character.class</code> - {@link CharacterConverter}</li>
     * <li><code>Double.class</code> - {@link DoubleConverter}</li>
     * <li><code>Float.class</code> - {@link FloatConverter}</li>
     * <li><code>Integer.class</code> - {@link IntegerConverter}</li>
     * <li><code>Long.class</code> - {@link LongConverter}</li>
     * <li><code>Short.class</code> - {@link ShortConverter}</li>
     * <li><code>String.class</code> - {@link StringConverter}</li>
     * </ul>
     *
     * @param throwException <code>true</code> if the converters should throw an exception
     *                       when a conversion error occurs, otherwise <code>
     *                       <code>false</code> if a default value should be used.
     * @param defaultNull    <code>true</code>if the <i>standard</i> converters (see
     *                       {@link StandConvertUtilsBean#registerStandard(boolean, boolean)}
     *                       ) should use a default value of <code>null</code>, otherwise
     *                       <code>false</code>. N.B. This values is ignored if
     *                       <code>throwException</code> is <code>true</code>
     */
    private void registerStandard(boolean throwException, boolean defaultNull) {

        Number defaultNumber = defaultNull ? null : ZERO;
        BigDecimal bigDecDeflt = defaultNull ? null : new BigDecimal("0.0");
        BigInteger bigIntDeflt = defaultNull ? null : new BigInteger("0");
        Boolean booleanDefault = defaultNull ? null : Boolean.FALSE;
        Character charDefault = defaultNull ? null : SPACE;
        String stringDefault = defaultNull ? null : "";

        register(BigDecimal.class, throwException ? new BigDecimalConverter() : new BigDecimalConverter(bigDecDeflt));
        register(BigInteger.class, throwException ? new BigIntegerConverter() : new BigIntegerConverter(bigIntDeflt));
        register(Boolean.class, throwException ? new BooleanConverter() : new BooleanConverter(booleanDefault));
        register(Byte.class, throwException ? new ByteConverter() : new ByteConverter(defaultNumber));
        register(Character.class, throwException ? new CharacterConverter() : new CharacterConverter(charDefault));
        register(Double.class, throwException ? new DoubleConverter() : new DoubleConverter(defaultNumber));
        register(Float.class, throwException ? new FloatConverter() : new FloatConverter(defaultNumber));
        register(Integer.class, throwException ? new IntegerConverter() : new IntegerConverter(defaultNumber));
        register(Long.class, throwException ? new LongConverter() : new LongConverter(defaultNumber));
        register(Short.class, throwException ? new ShortConverter() : new ShortConverter(defaultNumber));
        register(String.class, throwException ? new StringConverter() : new StringConverter(stringDefault));

    }

    /**
     * Register the converters for other types. </p> This method registers the
     * following converters:
     * <ul>
     * <li><code>Class.class</code> - {@link ClassConverter}</li>
     * <li><code>java.util.Date.class</code> - {@link DateConverter}</li>
     * <li><code>java.util.Calendar.class</code> - {@link CalendarConverter}</li>
     * <li><code>File.class</code> - {@link FileConverter}</li>
     * <li><code>java.sql.Date.class</code> - {@link SqlDateConverter}</li>
     * <li><code>java.sql.Time.class</code> - {@link SqlTimeConverter}</li>
     * <li><code>java.sql.Timestamp.class</code> - {@link SqlTimestampConverter}
     * </li>
     * <li><code>URL.class</code> - {@link URLConverter}</li>
     * </ul>
     *
     * @param throwException <code>true</code> if the converters should throw an exception
     *                       when a conversion error occurs, otherwise <code>
     *                       <code>false</code> if a default value should be used.
     */
    private void registerOther(boolean throwException) {
        register(Class.class, throwException ? new ClassConverter() : new ClassConverter(null));
        register(java.util.Date.class, throwException ? new DateConverter() : new DateConverter(null));
        register(Calendar.class, throwException ? new CalendarConverter() : new CalendarConverter(null));
        register(File.class, throwException ? new FileConverter() : new FileConverter(null));
        register(java.sql.Date.class, throwException ? new SqlDateConverter() : new SqlDateConverter(null));
        register(java.sql.Time.class, throwException ? new SqlTimeConverter() : new SqlTimeConverter(null));
        register(Timestamp.class, throwException ? new SqlTimestampConverter() : new SqlTimestampConverter(null));
        register(URL.class, throwException ? new URLConverter() : new URLConverter(null));
    }

    /**
     * Register array converters.
     *
     * @param throwException   <code>true</code> if the converters should throw an exception
     *                         when a conversion error occurs, otherwise <code>
     *                         <code>false</code> if a default value should be used.
     * @param defaultArraySize The size of the default array value for array converters (N.B.
     *                         This values is ignored if <code>throwException</code> is
     *                         <code>true</code>). Specifying a value less than zero causes a
     *                         <code>null<code> value to be used for
     *                         the default.
     */
    private void registerArrays(boolean throwException, int defaultArraySize) {

        // Primitives
        registerArrayConverter(Boolean.TYPE, new BooleanConverter(), throwException, defaultArraySize);
        registerArrayConverter(Byte.TYPE, new ByteConverter(), throwException, defaultArraySize);
        registerArrayConverter(Character.TYPE, new CharacterConverter(), throwException, defaultArraySize);
        registerArrayConverter(Double.TYPE, new DoubleConverter(), throwException, defaultArraySize);
        registerArrayConverter(Float.TYPE, new FloatConverter(), throwException, defaultArraySize);
        registerArrayConverter(Integer.TYPE, new IntegerConverter(), throwException, defaultArraySize);
        registerArrayConverter(Long.TYPE, new LongConverter(), throwException, defaultArraySize);
        registerArrayConverter(Short.TYPE, new ShortConverter(), throwException, defaultArraySize);

        // Standard
        registerArrayConverter(BigDecimal.class, new BigDecimalConverter(), throwException, defaultArraySize);
        registerArrayConverter(BigInteger.class, new BigIntegerConverter(), throwException, defaultArraySize);
        registerArrayConverter(Boolean.class, new BooleanConverter(), throwException, defaultArraySize);
        registerArrayConverter(Byte.class, new ByteConverter(), throwException, defaultArraySize);
        registerArrayConverter(Character.class, new CharacterConverter(), throwException, defaultArraySize);
        registerArrayConverter(Double.class, new DoubleConverter(), throwException, defaultArraySize);
        registerArrayConverter(Float.class, new FloatConverter(), throwException, defaultArraySize);
        registerArrayConverter(Integer.class, new IntegerConverter(), throwException, defaultArraySize);
        registerArrayConverter(Long.class, new LongConverter(), throwException, defaultArraySize);
        registerArrayConverter(Short.class, new ShortConverter(), throwException, defaultArraySize);
        registerArrayConverter(String.class, new StringConverter(), throwException, defaultArraySize);

        // Other
        registerArrayConverter(Class.class, new ClassConverter(), throwException, defaultArraySize);
        registerArrayConverter(java.util.Date.class, new DateConverter(), throwException, defaultArraySize);
        registerArrayConverter(Calendar.class, new DateConverter(), throwException, defaultArraySize);
        registerArrayConverter(File.class, new FileConverter(), throwException, defaultArraySize);
        registerArrayConverter(java.sql.Date.class, new SqlDateConverter(), throwException, defaultArraySize);
        registerArrayConverter(java.sql.Time.class, new SqlTimeConverter(), throwException, defaultArraySize);
        registerArrayConverter(Timestamp.class, new SqlTimestampConverter(), throwException, defaultArraySize);
        registerArrayConverter(URL.class, new URLConverter(), throwException, defaultArraySize);

    }

    /**
     * Register a new ArrayConverter with the specified element delegate
     * converter that returns a default array of the specified size in the event
     * of conversion errors.
     *
     * @param componentType      The component type of the array
     * @param componentConverter The converter to delegate to for the array elements
     * @param throwException     Whether a conversion exception should be thrown or a default
     *                           value used in the event of a conversion error
     * @param defaultArraySize   The size of the default array
     */
    private void registerArrayConverter(Class componentType, Converter componentConverter, boolean throwException, int defaultArraySize) {
        Class arrayType = Array.newInstance(componentType, 0).getClass();
        Converter arrayConverter = null;
        if (throwException) {
            arrayConverter = new ArrayConverter(arrayType, componentConverter);
        } else {
            arrayConverter = new ArrayConverter(arrayType, componentConverter, defaultArraySize);
        }
        register(arrayType, arrayConverter);
    }

    /**
     * strictly for convenience since it has same parameter order as Map.put
     */
    private void register(Class clazz, Converter converter) {
        register(new ConverterFacade(converter), clazz);
    }

    /**
     * Remove any registered {@link Converter} for the specified destination
     * <code>Class</code>.
     *
     * @param clazz Class for which to remove a registered Converter
     */
    public void deregister(Class clazz) {

        converters.remove(clazz);

    }

    /**
     * Look up and return any registered {@link Converter} for the specified
     * destination class; if there is no registered Converter, return
     * <code>null</code>.
     *
     * @param clazz Class for which to return a registered Converter
     * @return The registered {@link Converter} or <code>null</code> if not
     * found
     */
    public Converter lookup(Class clazz) {

        return ((Converter) converters.get(clazz));

    }

    /**
     * Look up and return any registered {@link Converter} for the specified
     * source and destination class; if there is no registered Converter, return
     * <code>null</code>.
     *
     * @param sourceType Class of the value being converted
     * @param targetType Class of the value to be converted to
     * @return The registered {@link Converter} or <code>null</code> if not
     * found
     */
    public Converter lookup(Class sourceType, Class targetType) {

        if (targetType == null) {
            throw new IllegalArgumentException("Target type is missing");
        }
        if (sourceType == null) {
            return lookup(targetType);
        }

        Converter converter = null;
        // Convert --> String
        if (targetType == String.class) {
            converter = lookup(sourceType);
            if (converter == null && (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType))) {
                converter = lookup(String[].class);
            }
            if (converter == null) {
                converter = lookup(String.class);
            }
            return converter;
        }

        // Convert --> String array
        if (targetType == String[].class) {
            if (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType)) {
                converter = lookup(sourceType);
            }
            if (converter == null) {
                converter = lookup(String[].class);
            }
            return converter;
        }

        return lookup(targetType);

    }

    /**
     * Register a custom {@link Converter} for the specified destination
     * <code>Class</code>, replacing any previously registered Converter.
     *
     * @param converter Converter to be registered
     * @param clazz     Destination class for conversions performed by this Converter
     */
    public void register(Converter converter, Class clazz) {

        converters.put(clazz, converter);

    }
}
