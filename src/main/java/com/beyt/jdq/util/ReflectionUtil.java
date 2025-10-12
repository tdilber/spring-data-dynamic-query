package com.beyt.jdq.util;

import com.beyt.jdq.util.field.FieldUtil;
import org.apache.commons.lang3.SerializationException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by tdilber at 7/10/2020
 */
public class ReflectionUtil {

    public static void convertObjectArrayToIfNotAvailable(Class<?> clazz, Object[] objects) throws Exception {
        convertObjectArrayToIfEnum(clazz, objects);
        convertObjectArrayToIfDate(clazz, objects);
    }

    public static void convertObjectArrayToIfEnum(Class<?> clazz, Object[] objects) throws Exception {
        if (objects.length > 0 && clazz.isEnum()) {
            Method valueOf = clazz.getMethod(
                    "valueOf", String.class);
            for (int i = 0; i < objects.length; i++) {
                objects[i] = valueOf.invoke(null, objects[i].toString());
            }
        }
    }

    public static void convertObjectArrayToIfDate(Class<?> clazz, Object[] objects) throws Exception {
        if (objects.length > 0 && Date.class.isAssignableFrom(clazz)) {
            for (int i = 0; i < objects.length; i++) {
                objects[i] = new Date(Long.parseLong(objects[i].toString()));
            }
        }
    }


    public static List<Object> convertObjectArrayToIfNotAvailable(Class<?> clazz, List<Object> objects) throws Exception {
        if (FieldUtil.isSupportedType(clazz)) {
            List<Object> result = new ArrayList<>();
            for (Object object : objects) {
                result.add(FieldUtil.fillValue(clazz, object.toString()));
            }
            return result;
        }

        return objects;
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserializeObject(String serialized, Class<T> clazz) throws Exception {
        if (FieldUtil.isSupportedType(clazz)) {
            return (T) FieldUtil.fillValue(clazz, serialized);
        }

        throw new SerializationException("Field Type: " + clazz.getName() + " not supported!");
    }
}
