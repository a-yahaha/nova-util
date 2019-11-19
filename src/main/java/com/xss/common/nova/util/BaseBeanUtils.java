package com.xss.common.nova.util;

import com.google.common.base.Preconditions;
import com.rits.cloning.Cloner;
import jodd.bean.BeanCopy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Iterator;
import java.util.Map;

@Slf4j
public class BaseBeanUtils {
    private static final BeanUtilsBean beanUtil = BeanUtilsBean.getInstance();
    private static final Cloner cloner = new Cloner();

    /**
     * 将源对象中所有与目的对象同名的属性拷贝到目的对象, 即使源对象中该属性的值为null
     */
    public static void copyProperties(Object src, Object des) {
        Preconditions.checkArgument(src != null, "源对象不能为空");
        Preconditions.checkArgument(des != null, "目标对象不能为空");
        BeanCopy.beans(src, des).copy();
    }

    /**
     * 将源对象中所有与目的对象同名的属性拷贝到目的对象, 除非源对象中该属性的值为null
     */
    public static void copyNoneNullProperties(Object src, Object des) {
        Preconditions.checkArgument(src != null, "源对象不能为空");
        Preconditions.checkArgument(des != null, "目标对象不能为空");
        BeanCopy.beans(src, des).ignoreNulls(true).copy();
    }

    /**
     * 浅拷贝
     */
    public static <T> T shallowClone(T src) {
        Preconditions.checkArgument(src != null, "对象不能为空");
        try {
            return (T) beanUtil.cloneBean(src);
        } catch (Exception e) {
            throw new RuntimeException("对类型为'{" + src.getClass().getName() + "}'进行浅拷贝失败.");
        }
    }

    /**
     * 深拷贝. 不可变的属性如String, 不会被拷贝
     */
    public static <T> T deepClone(T src) {
        Preconditions.checkArgument(src != null, "对象不能为空");
        return cloner.deepClone(src);
    }

    /**
     * 将对象转换为一个Map对象, key为对象的属性名, value为对象的值
     */
    public static Map<String, Object> beanToMap(Object obj) {
        Preconditions.checkArgument(obj != null, "对象不能为空");
        if (obj instanceof Map) {
            return (Map) obj;
        }

        try {
            Map<String, Object> map = PropertyUtils.describe(obj);
            map.remove("class");
            return map;
        } catch (Throwable e) {
            log.error("将对象[{}]转换为map异常: {}", BaseJsonUtils.writeValue(obj), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(String.format("将对象[%s]转换为map异常", BaseJsonUtils.writeValue(obj)));
        }
    }

    /**
     * 将对象转换为一个Map对象, key为对象的属性名, value为对象的值
     * value为null的属性不会包含在返回的Map对象中
     */
    public static Map<String, Object> beanToMapNonNull(Object obj) {
        Map<String, Object> map = beanToMap(obj);

        //遍历map, 将value为null的entry删除
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() == null) {
                iterator.remove();
            }
        }
        return map;
    }

    public static <T> T newInstance(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable e) {
            log.error("无法实例化类[{}]对象:{}", cls.getSimpleName(), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public static <T> T convert(Object obj, Class<T> cls) {
        T t = newInstance(cls);
        if (t != null) {
            copyNoneNullProperties(obj, t);
        }
        return t;
    }
}
