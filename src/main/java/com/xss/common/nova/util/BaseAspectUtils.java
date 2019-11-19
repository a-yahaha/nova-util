package com.xss.common.nova.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.annotation.LogIgnore;
import com.xss.common.nova.annotation.LogInfo;
import com.xss.common.nova.exception.AspectException;
import com.xss.common.nova.serializer.CustomSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class BaseAspectUtils {
    private static final Set<Class> jsonIgnoredClasses = BaseJsonUtils.jsonIgnoredClasses;
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        JacksonAnnotationIntrospector introspector = new JacksonAnnotationIntrospector() {
            @Override
            protected boolean _isIgnorable(Annotated a) {
                if (a.getAnnotation(LogIgnore.class) != null) {
                    return true;
                }
                return super._isIgnorable(a);
            }

            @Override
            public Object findSerializer(Annotated a) {
                JsonMosaic jsonMosaic = a.getAnnotation(JsonMosaic.class);
                if (jsonMosaic != null) {
                    return new CustomSerializer(jsonMosaic);
                }
                return super.findSerializer(a);
            }
        };
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setAnnotationIntrospector(introspector);
        mapper.registerModule(BaseJsonUtils.getSimpleModule());
    }

    public static Object logAround(ProceedingJoinPoint joinPoint, Long maxTimeInMillis, boolean... logAsDebug) throws Throwable {
        final long start = System.currentTimeMillis();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Map<String, String> paramValues = paramsMap(method, joinPoint.getArgs());
        String methodInfo = methodInfo(method, paramValues);

        if (logAsDebug != null && logAsDebug.length == 1 && logAsDebug[0]) {
            log.debug(methodInfo + "-开始");
        } else {
            log.info(methodInfo + "-开始");
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            String message = e.getMessage();
            if (StringUtils.isNotBlank(message)) {
                message = message.replaceFirst("false:", "");
            }

            LogInfo logInfo = method.getAnnotation(LogInfo.class);
            if (!(e instanceof IgnorableException)) {
                StringBuilder methodDetail = new StringBuilder(method.getDeclaringClass().getSimpleName() + "." + method.getName());
                if (joinPoint.getArgs() != null) {
                    methodDetail.append("(").append(paramInfo(method, joinPoint.getArgs())).append(")");
                }

                if (StringUtils.startsWithIgnoreCase(e.getMessage(), "false:") ||
                        (logInfo != null && !logInfo.logError())) {
                    log.warn("方法{}调用异常", methodDetail.toString());
                } else {
                    log.error("方法{}调用异常", methodDetail.toString());
                }
            }

            if (logInfo != null && (StringUtils.isNotBlank(logInfo.errorKey()) || !logInfo.logError())) {
                if (logInfo.errorParams().length > 0) {
                    String[] params = new String[logInfo.errorParams().length + 1];
                    IntStream.range(0, logInfo.errorParams().length)
                            .forEach(index -> params[index] = paramValues.get(logInfo.errorParams()[index]));
                    params[logInfo.errorParams().length] = message;
                    throw AspectException.fromKey(logInfo.errorKey(), logInfo.logError(), params);
                } else {
                    throw AspectException.fromKey(logInfo.errorKey(), logInfo.logError(), message);
                }
            }

            throw e;
        } finally {
            long timeUsed = System.currentTimeMillis() - start;
            if (maxTimeInMillis > 0 && timeUsed > maxTimeInMillis) {
                log.warn("{}-结束, 所花时间: {}ms", methodInfo, timeUsed);
            } else {
                log.debug("{}-结束, 所花时间: {}ms", methodInfo, timeUsed);
            }
        }
    }

    private static String methodInfo(Method method, Map<String, String> paramValues) {
        StringBuilder buf = new StringBuilder("方法").append(method.getDeclaringClass().getSimpleName()).append(".").append(method.getName());
        try {
            String info = "";
            LogInfo logInfo = method.getAnnotation(LogInfo.class);
            if (logInfo != null) {
                info = logInfo.value();
                if (logInfo.params().length > 0) {
                    for (String param : logInfo.params()) {
                        String paramValue = paramValues.get(param);
                        info = info.replaceFirst("\\{\\}", paramValue == null ? "" : Matcher.quoteReplacement(paramValue));
                    }
                }
            }

            if (StringUtils.isNotBlank(info)) {
                if (logInfo.replace()) {
                    buf = new StringBuilder(info);
                } else {
                    buf.append(":").append(info);
                }
            }
        } catch (Throwable e) {
            log.error("获取[{}]额外日志信息异常:{}", method, ExceptionUtils.getStackTrace(e));
        }
        return buf.toString();
    }

    private static String paramInfo(Method method, Object[] args) {
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            Parameter[] params = method.getParameters();
            IntStream.range(0, args.length).forEach(index -> {
                if (!(args[index] instanceof InputStream)) {
                    String sParam = "";
                    if (args[index] != null && params[index].getAnnotation(LogIgnore.class) == null) {
                        if (BaseObjectUtils.isPrimitive(args[index].getClass()) && params[index].getAnnotation(JsonMosaic.class) != null) {
                            JsonMosaic jsonMosaic = params[index].getAnnotation(JsonMosaic.class);
                            sParam = BaseJsonUtils.writeValue(args[index]);
                            sParam = BaseStringUtils.mosaic(sParam, jsonMosaic, '*');
                        } else {
                            sParam = toJson(args[index]);
                        }
                    }

                    if (index != 0) {
                        buf.append(", ");
                    }
                    buf.append(sParam);
                }
            });
        }

        return buf.toString();
    }

    private static Map<String, String> paramsMap(Method method, Object[] args) {
        Map<String, String> map = new HashMap<>();
        if (args != null) {
            Parameter[] params = method.getParameters();
            IntStream.range(0, args.length).forEach(index -> {
                try {
                    String name = index + "";
                    if (params[index].isNamePresent()) {
                        name = params[index].getName();
                    }
                    map.put(name, toJson(args[index]));

                    if (args[index] != null && params[index].getAnnotation(LogIgnore.class) == null) {
                        if (BaseObjectUtils.isPrimitive(args[index].getClass()) && params[index].getAnnotation(JsonMosaic.class) != null) {
                            JsonMosaic jsonMosaic = params[index].getAnnotation(JsonMosaic.class);
                            String text = BaseJsonUtils.writeValue(args[index]);
                            text = BaseStringUtils.mosaic(text, jsonMosaic, '*');
                            map.put(name, text);
                        } else {
                            map.putAll(toMap(name, args[index]));
                        }
                    }
                } catch (Throwable e) {
                    log.error("将对象[{}]转为map异常:{}", args[index].getClass().getSimpleName(), ExceptionUtils.getStackTrace(e));
                }
            });
        }

        return map;
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            return mapper.writeValueAsString(obj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> toMap(String name, Object value) {
        return toMap(name, value, 0);
    }

    private static Map<String, String> toMap(String name, Object value, int crtDepth) {
        Map<String, String> map = new HashMap<>();
        if (value != null && crtDepth < 5) {
            if (BaseObjectUtils.isPrimitive(value.getClass())) {
                map.put(name, value.toString());
            } else if (value instanceof Enum) {
                map.put(name, ((Enum) value).name());
            } else if (!(value instanceof Collection) && !(value instanceof Map)) {
                Class ignoredCls = jsonIgnoredClasses.stream().filter(cls -> cls.isAssignableFrom(value.getClass())).findAny().orElse(null);
                if (ignoredCls != null) {
                    map.put(name, value.getClass().getSimpleName());
                } else {
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

                            if (fieldValue != null && field.getAnnotation(LogIgnore.class) == null) {
                                if (BaseObjectUtils.isPrimitive(fieldValue.getClass()) && field.getAnnotation(JsonMosaic.class) != null) {
                                    JsonMosaic jsonMosaic = field.getAnnotation(JsonMosaic.class);
                                    String text = BaseJsonUtils.writeValue(fieldValue);
                                    fieldValue = BaseStringUtils.mosaic(text, jsonMosaic, '*');
                                }
                                map.put(StringUtils.isBlank(name) ? field.getName() : name + "." + field.getName(), toJson(fieldValue));
                                map.putAll(toMap(StringUtils.isBlank(name) ? field.getName() : name + "." + field.getName(), fieldValue, crtDepth + 1));
                            }
                        });
                    }
                }
            } else if (value instanceof Collection && ((Collection) value).size() > 0) {
                Map<String, String> temp = new HashMap<>();
                List<Map<String, String>> list = new ArrayList();
                ((Collection) value).forEach(item -> list.add(toMap("", item)));

                list.forEach(innerMap ->
                        innerMap.entrySet().forEach(entry -> {
                            String key = entry.getKey();
                            if (temp.containsKey(key)) {
                                temp.put(key, temp.get(key) + "," + entry.getValue());
                            } else {
                                temp.put(key, entry.getValue());
                            }
                        })
                );

                temp.forEach((tempKey, tempValue) ->
                        map.put(StringUtils.isBlank(name) ? tempKey : name + "." + tempKey, "[" + tempValue + "]"));
            }
        }

        return map;
    }

    public static class IgnorableException extends RuntimeException {
        public IgnorableException() {

        }

        public IgnorableException(Throwable e) {
            super(e);
        }
    }
}
