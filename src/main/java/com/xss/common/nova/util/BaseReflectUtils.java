package com.xss.common.nova.util;

import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BaseReflectUtils {
    public static Object parse(String src, Type type) {
        Type rawType = type;
        boolean isArray = false;
        if (type instanceof ParameterizedType) {
            isArray = true;
            rawType = ((ParameterizedType) type).getRawType();
            if (!(rawType instanceof Class) || (rawType != List.class && rawType != Set.class)) {
                log.info("无法将{}转换为不支持的类型{}", src, type);
                return null;
            }
        }

        Class cls = getClass(type);
        if (isArray) {
            Stream stream = BaseJsonUtils.readValues(src, String.class).stream()
                    .map(innerSrc -> BaseReflectUtils.parse(innerSrc, cls));

            if (rawType == List.class) {
                return stream.collect(Collectors.toList());
            } else {
                return stream.collect(Collectors.toSet());
            }
        }

        //将src解析为基本类型对象
        PropertyEditor editor = PropertyEditorManager.findEditor(cls);
        if (editor != null) {
            editor.setAsText(src);
            return editor.getValue();
        }

        //将src解析为Date对象
        if (cls == Date.class || cls == java.sql.Date.class) {
            return BaseDateUtils.parseDate(src);
        }

        try {
            return BaseJsonUtils.readValue(src, cls);
        } catch (Throwable e) {
            log.error("无法将{}解析为{}类型的对象", src, type);
            return null;
        }
    }

    private static Class getClass(Type type) {
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }
        return (Class) type;
    }
}
