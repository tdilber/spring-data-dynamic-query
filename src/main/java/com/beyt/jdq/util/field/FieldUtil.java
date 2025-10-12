package com.beyt.jdq.util.field;

import com.beyt.jdq.util.field.helper.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.Timestamp;

/**
 * Created by tdilber at 11/17/2020
 */
public final class FieldUtil {
    private FieldUtil() {

    }

    private static final Map<Class<?>, IFieldHelper<?>> fieldHelperMap = new HashMap<>();

    static {
        fieldHelperMap.put(String.class, new StringFieldHelper());
        fieldHelperMap.put(Boolean.class, new BooleanFieldHelper());
        fieldHelperMap.put(Date.class, new DateFieldHelper());
        fieldHelperMap.put(Timestamp.class, new TimestampFieldHelper());
        fieldHelperMap.put(Double.class, new DoubleFieldHelper());
        fieldHelperMap.put(Long.class, new LongFieldHelper());
        fieldHelperMap.put(LocalDate.class, new LocalDateFieldHelper());
        fieldHelperMap.put(ZonedDateTime.class, new ZonedDateTimeFieldHelper());
        fieldHelperMap.put(Instant.class, new InstantFieldHelper());
        fieldHelperMap.put(Integer.class, new IntegerFieldHelper());
        fieldHelperMap.put(BigDecimal.class, new BigDecimalFieldHelper());
    }

    private static IFieldHelper<?> getFieldHelper(Class<?> fieldType) {
        for (Class<?> clazz : fieldHelperMap.keySet()) {
            if (fieldType.isAssignableFrom(clazz) && fieldHelperMap.containsKey(clazz)) {
                return fieldHelperMap.get(clazz);
            }
        }
        throw new IllegalStateException("Field Type: " + fieldType.getName() + " not supported!");
    }

    public static boolean isSupportedType(Class<?> clazz) {
        return clazz.isEnum() || fieldHelperMap.containsKey(clazz);
    }

    public static Object fillRandom(Class<?> fieldType) {
        if (fieldType.isEnum()) {
            return new EnumFieldHelper(fieldType).fillRandom();
        }
        return getFieldHelper(fieldType).fillRandom();
    }

    public static Object fillValue(Class<?> fieldType, String value) {
        if (fieldType.isEnum()) {
            return new EnumFieldHelper(fieldType).fillValue(value);
        }
        return getFieldHelper(fieldType).fillValue(value);
    }

    public static String createGeneratorCode(Class<?> fieldType, String value) {
        if (fieldType.isEnum()) {
            return new EnumFieldHelper(fieldType).createGeneratorCode(value);
        }
        return getFieldHelper(fieldType).createGeneratorCode(value);
    }
}
