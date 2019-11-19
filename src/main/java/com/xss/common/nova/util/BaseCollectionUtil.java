package com.xss.common.nova.util;

import com.google.common.base.Preconditions;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class BaseCollectionUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    public static boolean contains(Collection<?> collection, Object obj) {
        if (isEmpty(collection)) {
            return false;
        }
        return collection.stream().filter(item -> ObjectUtils.nullSafeEquals(item, obj)).findAny().isPresent();
    }

    public static boolean containsIgnoreCase(Collection<String> collection, String s) {
        if (isEmpty(collection)) {
            return false;
        }

        return collection.stream().filter(item -> (item == null && s == null) || item.equalsIgnoreCase(s))
                .findAny().isPresent();
    }

    public static void add(List list, int index, Object ele) {
        Preconditions.checkNotNull(list, "list参数不能为null");
        if (index < list.size()) {
            list.set(index, ele);
            return;
        }
        if (index > list.size()) {
            IntStream.range(list.size(), index).forEach(i -> list.add(i, null));
        }
        list.add(ele);
    }

    public static <T> void shuffleList(List<T> list) {
        int size = list.size();
        Random random = new Random();
        random.nextInt();

        for (int i = 0; i < size; i++) {
            int change = i + random.nextInt(size - i);
            swap(list, i, change);
        }
    }

    public static <T, R> T get(Map<R, T> map, R key, T defaultValue) {
        Preconditions.checkNotNull(map, "map不能为null");

        if(!map.containsKey(key)) {
            map.put(key, defaultValue);
            return defaultValue;
        }

        return map.get(key);
    }

    public static int size(Collection<?> collection) {
        if(collection == null) {
            return 0;
        }

        return collection.size();
    }

    public static int size(Map map) {
        if(map == null) {
            return 0;
        }

        return map.size();
    }

    private static <T> void swap(List<T> list, int i, int change) {
        T temp = list.get(i);
        list.set(i, list.get(change));
        list.set(change, temp);
    }
}
