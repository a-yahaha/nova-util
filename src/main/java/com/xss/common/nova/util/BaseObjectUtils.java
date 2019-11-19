package com.xss.common.nova.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.xss.common.nova.annotation.JsonMosaic;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class BaseObjectUtils {
    private static final List<Class<?>> PRIMITIVE = new ArrayList<>();
    private static final Map<String, Class> EXTRA_NAME_TO_CLASS = ImmutableMap.of(
            "decimal", BigDecimal.class,
            "bool", Boolean.class,
            "time", Date.class,
            "datetime", Date.class);

    static {
        PRIMITIVE.add(Character.class);
        PRIMITIVE.add(String.class);
        PRIMITIVE.add(Boolean.class);
        PRIMITIVE.add(Byte.class);
        PRIMITIVE.add(Short.class);
        PRIMITIVE.add(Integer.class);
        PRIMITIVE.add(Long.class);
        PRIMITIVE.add(Float.class);
        PRIMITIVE.add(Double.class);
        PRIMITIVE.add(BigDecimal.class);
        PRIMITIVE.add(Date.class);
        PRIMITIVE.add(boolean.class);
        PRIMITIVE.add(byte.class);
        PRIMITIVE.add(short.class);
        PRIMITIVE.add(int.class);
        PRIMITIVE.add(long.class);
        PRIMITIVE.add(float.class);
        PRIMITIVE.add(double.class);
        PRIMITIVE.add(char.class);
    }

    public static boolean isPrimitive(Type type) {
        if (type instanceof ParameterizedType) {
            return isPrimitive(((ParameterizedType) type).getActualTypeArguments()[0]);
        }

        return PRIMITIVE.contains(type);
    }

    /**
     * 举例name="user", value对应的对象的json格式为{"name": "brady", "age": 18, "addr": {"country": "china"}}, 返回的map为
     * user.name = brady
     * user.age = 18
     * user.addr.country = china
     */
    public static Map<String, String> toMap(String name, Object value) {
        Map<String, String> map = new HashMap<>();
        if (value != null) {
            if (PRIMITIVE.contains(value.getClass()) || value.getClass().isPrimitive()) {
                map.put(name, value.toString());
            } else if (value instanceof Enum) {
                map.put(name, ((Enum) value).name());
            } else if (!(value instanceof Collection) && !(value instanceof Map)) {
                Field[] fields = value.getClass().getDeclaredFields();
                if (fields != null) {
                    Stream.of(fields).forEach(field -> {
                        Object fieldValue = null;
                        try {
                            field.setAccessible(true);
                            fieldValue = field.get(value);
                        } catch (Throwable e) {
                            log.warn("无法访问类[{}]成员变量[{}]", field.getClass().getSimpleName(), field.getName());
                        }

                        if (fieldValue != null) {
                            if (BaseObjectUtils.isPrimitive(fieldValue.getClass()) && field.getAnnotation(JsonMosaic.class) != null) {
                                JsonMosaic jsonMosaic = field.getAnnotation(JsonMosaic.class);
                                String text = BaseJsonUtils.writeValue(fieldValue);
                                fieldValue = BaseStringUtils.mosaic(text, jsonMosaic, '*');
                            }
                            map.putAll(toMap(StringUtils.isBlank(name) ? field.getName() : name + "." + field.getName(), fieldValue));
                        }
                    });
                }
            }
        }

        return map;
    }

    public static Class forName(String name) {
        return forName(name, false);
    }

    public static Class forName(String name, boolean returnWrapper) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        if (EXTRA_NAME_TO_CLASS.containsKey(name.toLowerCase())) {
            return EXTRA_NAME_TO_CLASS.get(name.toLowerCase());
        }

        Class result = PRIMITIVE.stream().filter(cls ->
                cls.getSimpleName().equalsIgnoreCase(name)
        ).findAny().orElse(null);

        if (result != null) {
            if (result.isPrimitive() && returnWrapper) {
                return Primitives.wrap(result);
            } else {
                return result;
            }
        }

        try {
            return Class.forName(name);
        } catch (Throwable e) {
            return null;
        }
    }
}
