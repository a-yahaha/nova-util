package com.xss.common.nova.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.serializer.CustomSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
public class BaseJsonUtil {
    public static final Set<Class> jsonIgnoredClasses = ImmutableSet.of(ServletRequest.class, ServletResponse.class,
            InputStream.class, OutputStream.class, RequestBody.class, ResponseBody.class,
            okhttp3.RequestBody.class, okhttp3.ResponseBody.class, Environment.class, MultipartFile.class, File.class,
            Logger.class, java.util.logging.Logger.class, Class.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper mapperIgnoreUnknown = new ObjectMapper();
    private static final ObjectMapper mapperScanAnno = new ObjectMapper();
    private static SimpleModule module;
    private static String basePath;

    static {
        SimpleSerializers simpleSerializers = new SimpleSerializers();
        jsonIgnoredClasses.forEach(cls -> simpleSerializers.addSerializer(new ClassNameSerializer(cls)));
        module = new SimpleModule();
        module.setSerializers(simpleSerializers);

        JacksonAnnotationIntrospector introspector = new JacksonAnnotationIntrospector() {
            @Override
            public Object findSerializer(Annotated a) {
                JsonMosaic jsonMosaic = a.getAnnotation(JsonMosaic.class);
                if (jsonMosaic != null) {
                    return new CustomSerializer(jsonMosaic);
                }
                return super.findSerializer(a);
            }
        };

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        mapper.registerModule(module);

        mapperIgnoreUnknown.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapperIgnoreUnknown.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapperIgnoreUnknown.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapperIgnoreUnknown.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        mapperIgnoreUnknown.registerModule(module);

        mapperScanAnno.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapperScanAnno.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapperScanAnno.configure(FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapperScanAnno.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        mapperScanAnno.setAnnotationIntrospector(introspector);
        mapperScanAnno.registerModule(module);

        URL baseUrl = BaseJsonUtil.class.getClassLoader().getResource(".");
        try {
            if (baseUrl != null && StringUtils.isNotBlank(baseUrl.getPath())) {
                basePath = URLDecoder.decode(baseUrl.getPath(), "UTF-8");
                basePath = StringUtils.substringBefore(basePath, "/target");
            }
        } catch (Exception e) {
            log.info("获取项目路径异常");
        }
    }

    public static ObjectMapper defaultMaspper() {
        return mapperIgnoreUnknown;
    }

    public static <T> T readValue(String s, Class<T> cls) {
        return readValue(s, cls, true);
    }

    public static <T> T readValueChecked(String s, Class<T> cls) throws Exception {
        return readValueChecked(s, cls, true);
    }

    public static <T> T readValue(String s, Class<T> cls, boolean ignoreUnknown) {
        Preconditions.checkArgument(cls != null, "Class类型不能为空");
        if (cls == String.class) {
            return (T) s;
        }

        if (StringUtils.isBlank(s)) {
            return null;
        }

        try {
            if (ignoreUnknown) {
                return mapperIgnoreUnknown.readValue(s, cls);
            } else {
                return mapper.readValue(s, cls);
            }
        } catch (Throwable e) {
            log.error("无法将{}转换为类型为[{}]的对象: {}", s, cls.getSimpleName(), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public static <T> T readValueChecked(String s, Class<T> cls, boolean ignoreUnknown) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(s), "字串不能为空");
        Preconditions.checkArgument(cls != null, "Class类型不能为空");
        if (cls == String.class) {
            return (T) s;
        }

        if (ignoreUnknown) {
            return mapperIgnoreUnknown.readValue(s, cls);
        } else {
            return mapper.readValue(s, cls);
        }
    }

    public static <T> T readValue(String s, TypeReference typeReference) {
        return readValue(s, typeReference, true);
    }

    public static <T> T readValue(String s, TypeReference typeReference, boolean ignoreUnknown) {
        Preconditions.checkArgument(StringUtils.isNotBlank(s), "字串不能为空");
        Preconditions.checkArgument(typeReference != null, "TypeWrapper类型不能为空");

        try {
            if (ignoreUnknown) {
                return mapperIgnoreUnknown.readValue(s, typeReference);
            } else {
                return mapper.readValue(s, typeReference);
            }
        } catch (Throwable e) {
            log.error("无法将{}转换为类型为[{}]的对象: {}", s, typeReference.getType(), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public static <T> T readValue(Map<String, Object> map, Class<T> cls) {
        return readValue(map, cls, true);
    }

    public static <T> T readValue(Map<String, Object> map, Class<T> cls, boolean ignoreUnknown) {
        Preconditions.checkArgument(map != null, "map不能为null");
        Preconditions.checkArgument(cls != null, "Class类型不能为空");
        if (cls == String.class) {
            return (T) BaseJsonUtil.writeValue(map);
        }

        try {
            if (ignoreUnknown) {
                return mapperIgnoreUnknown.convertValue(map, cls);
            } else {
                return mapper.convertValue(map, cls);
            }
        } catch (Throwable e) {
            log.error("无法将Map{}转换为类型为[{}]的对象: {}", map, cls.getSimpleName(), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public static <T> List<T> readValues(String s, Class<T> cls) {
        return readValues(s, cls, true);
    }

    public static <T> List<T> readValues(String s, Class<T> cls, boolean ignoreUnknown) {
        Preconditions.checkArgument(StringUtils.isNotBlank(s), "字串不能为空");
        Preconditions.checkArgument(cls != null, "Class类型不能为空");

        List list;
        try {
            if (ignoreUnknown) {
                list = mapperIgnoreUnknown.readValue(s, List.class);
            } else {
                list = mapper.readValue(s, List.class);
            }
        } catch (Throwable e) {
            log.error("无法将{}转换为数组对象: {}", s, ExceptionUtils.getStackTrace(e));
            return Collections.emptyList();
        }

        if (cls == List.class) {
            return list;
        }

        return (List<T>) list.stream().map(ele -> {
            if (ele instanceof Map) {
                return readValue((Map) ele, cls);
            } else {
                return readValue(ele.toString(), cls);
            }
        }).collect(Collectors.toList());
    }

    public static String writeValue(Object obj) {
        return writeValue(obj, false);
    }

    public static String writeValue(Object obj, boolean scanAnno) {
        if (obj == null) {
            return null;
        }

        if (BaseObjectUtil.isPrimitive(obj.getClass())) {
            return obj.toString();
        }

        try {
            if (scanAnno) {
                return mapperScanAnno.writeValueAsString(obj);
            } else {
                return mapper.writeValueAsString(obj);
            }
        } catch (Throwable e) {
            log.error("json转换异常", e);
            return null;
        }
    }

    public static Object valueFromJsonKey(String json, String key) {
        Preconditions.checkArgument(StringUtils.isNotBlank(json), "json内容不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(key), "key不能为空");

        if (!json.startsWith("{") || !json.endsWith("}")) {
            json = StringUtils.substringBeforeLast(json, "}");
            json = StringUtils.substringAfter(json, "{");
            json = "{" + json + "}";
        }
        Map<String, Object> map = readValue(json, Map.class);
        return map.get(key);
    }

    public static SimpleModule getSimpleModule() {
        return module;
    }

    public static void generateCode(String rootClassName, String content, String packageName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(content), "内容不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(packageName), "包名不能为空");

        Map<String, Object> jsonMap;
        if (StringUtils.startsWith(content, "[")) {
            jsonMap = BaseJsonUtil.readValues(content, Map.class).get(0);
        } else {
            jsonMap = BaseJsonUtil.readValue(content, Map.class);
        }
        generateCode("", rootClassName, jsonMap, packageName);
    }

    private static String generateCode(String parent, String name, Map<String, Object> jsonMap, String packageName) {
        //确保文件夹存在
        File dir;
        try {
            dir = new File(basePath + "/src/main/java/" + packageName.replace('.', '/'));
            FileUtils.forceMkdir(dir);
        } catch (Throwable e) {
            log.error("生成文件夹异常");
            throw new RuntimeException(e);
        }

        //存在{"p1": {"item" : {...}}, "p2": {"item": {...}}},同时有多个item的情况
        //确定实际文件名
        FileInputStream in = null;
        name = BaseStringUtil.underScoreToCamel(name, true);
        File file = new File(dir, name + ".java");
        if (file.exists()) {
            //判断文件内容是否一样
            try {
                in = new FileInputStream(file);
                String fileContent = IOUtils.toString(in, Charsets.UTF_8);
                if (jsonMap.entrySet().stream().filter(entry -> !fileContent.contains(entry.getKey())).findAny().orElse(null) != null) {
                    name = BaseStringUtil.underScoreToCamel(parent, true) + name;
                    file = new File(dir, name + ".java");
                }
            } catch (Throwable e) {
                log.error("读取文件内容异常", e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        //确保文件存在
        try {
            FileUtils.touch(file);
        } catch (Throwable e) {
            log.error("json内容转文件异常", e);
        }

        //组装内容
        String fileName = name;
        StringBuilder bufFirst = new StringBuilder("package ").append(packageName).append(";\n\n");
        StringBuilder bufHeader = new StringBuilder("import lombok.Data;\n\n");
        AtomicBoolean hasExtraHeader = new AtomicBoolean(false);

        StringBuilder bufFooter = new StringBuilder("@Data\n");
        bufFooter.append("public class ").append(name).append(" {\n");
        jsonMap.entrySet().forEach(entry -> {
            JsonStructure jsonStructure = flatJsonValue(entry.getValue());
            String key = entry.getKey();
            String standardKey = BaseStringUtil.underScoreToCamel(key, false);
            Object value = jsonStructure.getValue();
            String type;
            if (value instanceof Map) {
                type = BaseStringUtil.underScoreToCamel(key, true);
                if (jsonStructure.getDepth() > 0) {
                    type = BaseStringUtil.singularize(type);
                    if (!StringUtils.endsWithIgnoreCase(standardKey, "list") && !StringUtils.endsWithIgnoreCase(standardKey, "set")) {
                        standardKey = BaseStringUtil.pluralize(standardKey);
                    }
                }
                type = generateCode(fileName, type, (Map) value, packageName);
            } else {
                if (value instanceof String) {
                    Class cls = BaseObjectUtil.forName((String) value, true);
                    type = cls == null ? "String" : cls.getSimpleName();
                } else {
                    type = value.getClass().getSimpleName();
                }
            }
            if (!key.equals(standardKey)) {
                if (bufFirst.indexOf("import com.fasterxml.jackson.annotation.JsonProperty") == -1) {
                    bufFirst.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
                }
                bufFooter.append("    @JsonProperty(\"").append(key).append("\")\n");
            }

            if (type.equals("BigDecimal") && bufHeader.indexOf("import java.math.BigDecimal") == -1) {
                hasExtraHeader.set(true);
                bufHeader.append("import java.math.BigDecimal;\n");
            } else if (type.equals("Date") && bufHeader.indexOf("import java.util.Date") == -1) {
                hasExtraHeader.set(true);
                bufHeader.append("import java.util.Date;\n");
            }
            if (jsonStructure.getDepth() > 0 && bufHeader.indexOf("import java.util.List") == -1) {
                hasExtraHeader.set(true);
                bufHeader.append("import java.util.List;\n");
            }
            bufFooter.append("    private ").append(wrapWithDepth(jsonStructure.getDepth(), type)).append(" ").append(standardKey).append(";\n");
        });
        bufFooter.append("}");

        if (hasExtraHeader.get()) {
            bufHeader.append("\n");
        }

        //将内容写入文件
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            IOUtils.write(bufFirst.append(bufHeader).append(bufFooter).toString(), out, Charsets.UTF_8);
        } catch (Throwable e) {
            log.error("json内容转文件异常", e);
        } finally {
            IOUtils.closeQuietly(out);
        }

        return fileName;
    }

    private static JsonStructure flatJsonValue(Object value) {
        if (value instanceof List) {
            Object item = ((List) value).get(0);
            JsonStructure jsonStructure = flatJsonValue(item);
            jsonStructure.setDepth(jsonStructure.getDepth() + 1);
            return jsonStructure;
        } else {
            return new JsonStructure(value);
        }
    }

    private static String wrapWithDepth(int depth, String type) {
        if (depth == 0) {
            return type;
        }

        StringBuilder buf = new StringBuilder("List");
        IntStream.range(0, depth).forEach(i -> buf.append("<"));
        buf.append(type);
        IntStream.range(0, depth).forEach(i -> buf.append(">"));
        return buf.toString();
    }


    @Data
    static class JsonStructure {
        private Object value;
        private int depth = 0;

        public JsonStructure(Object value) {
            this.value = value;
        }
    }

    public static class ClassNameSerializer extends StdSerializer {
        private Class cls;

        public ClassNameSerializer(Class cls) {
            super(cls);
            this.cls = cls;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeString("null");
            } else {
                gen.writeString(cls.getSimpleName());
            }
        }
    }
}
