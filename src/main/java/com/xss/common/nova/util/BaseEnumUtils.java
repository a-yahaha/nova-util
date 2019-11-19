package com.xss.common.nova.util;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;

public class BaseEnumUtils {
    public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String name) {
        return getEnum(enumClass, name, null);
    }

    public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String name, T defaultEnum) {
        if (StringUtils.isBlank(name)) {
            return defaultEnum;
        }

        for (T t : enumClass.getEnumConstants()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }

        return defaultEnum;
    }
}
