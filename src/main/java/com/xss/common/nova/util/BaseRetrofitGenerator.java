package com.xss.common.nova.util;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.squareup.javapoet.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.http.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BaseRetrofitGenerator {

    public static String PROFILE = "retrofit.profile";
    private static String HEADER = "header";
    private static String BASE_URL = "base_url";
    private static String QUERY = "query";
    private static String BODY = "body";
    private static String REQUEST_URL = "url";
    private static String REQUEST_METHOD = "request_method";

    private static String rootPath;
    private static String basePath;
    private static String configPath;
    private static String apiPath;
    private static String classPath;

    static {
        URL baseUrl = BaseRetrofitGenerator.class.getClassLoader().getResource(".");
        try {
            rootPath = URLDecoder.decode(baseUrl.getPath(), "UTF-8");
        } catch (Exception e) {
            log.error("获取项目路径异常", e);
        }

        if (rootPath.contains("/server/target")) {
            rootPath = org.apache.commons.lang3.StringUtils.substringBefore(rootPath, "/target");
        } else {
            log.error("BaseClientUtils方法必须在server模块下使用!");
        }
    }

    public static void generateJavaFileFromCurl(String cmd, String methodName, String rootPackage, boolean append) {
        BaseRetrofitGenerator.configPath = rootPackage;
        BaseRetrofitGenerator.apiPath = rootPackage;
        BaseRetrofitGenerator.classPath = rootPackage;
        methodName = methodName.trim();

        if (StringUtils.isNotBlank(rootPackage)) {
            if ("test".equalsIgnoreCase((String) BaseKeyValue.get(PROFILE))) {
                basePath = rootPath + "/src/test/java/";
            } else {
                basePath = rootPath + "/src/main/java/";
            }
            BaseRetrofitGenerator.configPath = basePath + rootPackage.replaceAll("\\.", "/") + "/config";
            BaseRetrofitGenerator.apiPath = basePath + rootPackage.replaceAll("\\.", "/") + "/api";
            BaseRetrofitGenerator.classPath = basePath + rootPackage.replaceAll("\\.", "/") + "/dto";
        }

        String configPackage = rootPackage + ".config";
        String apiPackage = rootPackage + ".api";
        String classPackage = rootPackage + ".dto";

        Multimap<String, String> paramsMap = parseCommand(cmd);
        if (!paramsMap.get(BODY).isEmpty()) {
            File dir = new File(classPath);
            if (!dir.exists()) {
                try {
                    FileUtils.forceMkdir(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            BaseJsonUtils.generateCode(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, methodName + "ReqDto"), paramsMap.get(BODY).toArray()[0].toString(), classPackage);
        }

        outputApiConfig(paramsMap.get(BASE_URL).toArray()[0].toString(), configPackage, apiPackage);
        outputApi(paramsMap, methodName, apiPackage, classPackage, append);


    }

    private static void outputApi(Multimap<String, String> paramsMap, String methodName, String apiPackage, String classPackage, boolean append) {
        String finalClientContent;
        if (append) {
            File file = new File(BaseRetrofitGenerator.apiPath + "/Api.java");
            if (!file.exists()) {
                finalClientContent = generateApi(methodName, apiPackage, classPackage, paramsMap).toString();
            } else {
                List<String> implementedMethodsNameList = getImplementedMethodsName(apiPackage);
                String originalContent = BaseFileUtils.fileToString(new File(apiPath + "/" + "Api.java"));
                if (!CollectionUtils.isNotEmpty(implementedMethodsNameList) || !implementedMethodsNameList.contains(methodName)) {
                    String newContent = generateApi(methodName, apiPackage, classPackage, paramsMap).toString();
                    finalClientContent = mergeApi(originalContent, newContent);
                } else {
                    finalClientContent = originalContent;
                }
            }
        } else {
            finalClientContent = generateApi(methodName, apiPackage, classPackage, paramsMap).toString();
        }

        BaseFileUtils.writeToFileSave(apiPath, "Api.java", finalClientContent, false);
    }

    private static void outputApiConfig(String baseUrl, String configPackage, String apiPackage) {
        String configContent = generateApiConfig(baseUrl, configPackage, apiPackage);
        if (StringUtils.isNotBlank(configContent)) {
            BaseFileUtils.writeToFileSave(configPath, "ApiConfig.java", configContent, false);
        }
    }

    private static String mergeApi(String originalContent, String newContent) {
        LinkedList<String> originalLine = Lists.newLinkedList(Splitter.on("\n").splitToList(originalContent));
        List<String> newline = Splitter.on("\n").splitToList(newContent.trim());
        List<String> importPackageList = newline.stream()
                .filter(x -> x.startsWith("import ") && x.endsWith(";"))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(importPackageList)) {
            List<String> originalImportPackageList = originalLine.stream()
                    .filter(x -> x.trim().startsWith("import ") && x.trim().endsWith(";"))
                    .collect(Collectors.toList());
            int importIndex = originalLine.indexOf(originalImportPackageList.get(0));
            importPackageList.stream()
                    .filter(x -> !originalImportPackageList.contains(x))
                    .forEach(x -> originalLine.add(importIndex, x));

        }

        int methodIndex = newline.stream()
                .filter(x -> x.trim().startsWith("@") && x.trim().endsWith(")"))
                .findFirst()
                .map(newline::indexOf)
                .orElse(0);

        int originalMethodIndex = originalLine.stream()
                .filter(x -> x.trim().startsWith("public interface") && x.trim().endsWith("{"))
                .findFirst()
                .map(originalLine::indexOf)
                .orElse(0) + 1;

        if (!originalLine.get(originalMethodIndex).trim().isEmpty()) {
            originalLine.add(originalMethodIndex, "\r\n");
        }

        int classLastIndex = originalLine.stream()
                .filter(x -> x.trim().equals("}"))
                .map(originalLine::indexOf)
                .sorted(Comparator.reverseOrder()).limit(1).findFirst().orElse(0);

        originalLine.add(classLastIndex, "\r\n");
        originalLine.addAll(classLastIndex + 1, newline.subList(methodIndex, newline.lastIndexOf("}")));

        StringBuilder s = new StringBuilder();
        originalLine.forEach(x -> append(s, x));
        return s.toString();


    }

    private static void append(StringBuilder s, String line) {
        if (org.apache.commons.lang3.StringUtils.isBlank(line.trim())) {
            s.append("\r\n");
        } else {
            s.append(line).append("\n");
        }
    }

    private static JavaFile generateApi(String methodName, String apiPackage, String classPackage, Multimap<String, String> paramsMap) {

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder("Api").addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder interfaceMethodBuilder = MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(ResponseBody.class);
        String requestMethod = paramsMap.get(REQUEST_METHOD).toArray()[0].toString();
        if ("get".equalsIgnoreCase(requestMethod)) {
            interfaceMethodBuilder.addAnnotation(AnnotationSpec.builder(GET.class)
                    .addMember("value", "\"" + paramsMap.get(REQUEST_URL).toArray()[0].toString() + "\"").build());
        } else if ("post".equalsIgnoreCase(requestMethod)) {
            interfaceMethodBuilder.addAnnotation(AnnotationSpec.builder(POST.class)
                    .addMember("value", "\"" + paramsMap.get(REQUEST_URL).toArray()[0].toString() + "\"").build());
        } else if ("put".equalsIgnoreCase(requestMethod)) {
            interfaceMethodBuilder.addAnnotation(AnnotationSpec.builder(PUT.class)
                    .addMember("value", "\"" + paramsMap.get(REQUEST_URL).toArray()[0].toString() + "\"").build());
        } else if ("patch".equalsIgnoreCase(requestMethod)) {
            interfaceMethodBuilder.addAnnotation(AnnotationSpec.builder(PATCH.class)
                    .addMember("value", "\"" + paramsMap.get(REQUEST_URL).toArray()[0].toString() + "\"").build());
        } else if ("delete".equalsIgnoreCase(requestMethod)) {
            interfaceMethodBuilder.addAnnotation(AnnotationSpec.builder(DELETE.class)
                    .addMember("value", "\"" + paramsMap.get(REQUEST_URL).toArray()[0].toString() + "\"").build());
        } else if ("head".equalsIgnoreCase(requestMethod)) {
            interfaceMethodBuilder.addAnnotation(AnnotationSpec.builder(HEAD.class)
                    .addMember("value", "\"" + paramsMap.get(REQUEST_URL).toArray()[0].toString() + "\"").build());
        }


        if (CollectionUtils.isNotEmpty(paramsMap.get(BODY))) {
            interfaceMethodBuilder.addParameter(ParameterSpec.builder(
                    ClassName.get(classPackage, CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, methodName + "ReqDto")), "reqDto")
                    .addAnnotation(AnnotationSpec.builder(Body.class).build())
                    .build());

        }
        if (CollectionUtils.isNotEmpty(paramsMap.get(HEADER))) {
            paramsMap.get(HEADER).forEach(x -> interfaceMethodBuilder.addParameter(ParameterSpec.builder(
                    String.class, x)
                    .addAnnotation(AnnotationSpec.builder(Header.class)
                            .addMember("value", "\"" + x + "\"").build())
                    .build()));
        }

        if (CollectionUtils.isNotEmpty(paramsMap.get(QUERY))) {
            paramsMap.get(QUERY).forEach(x -> interfaceMethodBuilder.addParameter(ParameterSpec.builder(
                    String.class, x)
                    .addAnnotation(AnnotationSpec.builder(Query.class)
                            .addMember("value", "\"" + x + "\"").build())
                    .build()));
        }

        interfaceBuilder.addMethod(interfaceMethodBuilder.build());

        return JavaFile.builder(apiPackage, interfaceBuilder.build()).indent("    ").build();
    }

    private static List<String> getImplementedMethodsName(String apiPackage) {
        List<String> implementedMethodsNameList = null;
        try {
            implementedMethodsNameList = Arrays
                    .stream(Class.forName(apiPackage + ".Api").getDeclaredMethods())
                    .map(Method::getName)
                    .collect(Collectors.toList());
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
        }
        return implementedMethodsNameList;

    }

    private static String generateApiConfig(String baseUrl, String configPackage, String apiPackage) {
        File file = new File(BaseRetrofitGenerator.configPath + "/ApiConfig.java");
        if (!file.exists()) {
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder("ApiConfig")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Configuration.class);

            classBuilder.addField(FieldSpec.builder(String.class, "apiBaseUrl", Modifier.PRIVATE)
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "\"{nova.api.baseUrl:" + baseUrl + "}\"").build()).build());

            MethodSpec.Builder retrofitMethod = MethodSpec.methodBuilder("retrofit")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Bean.class)
                    .addStatement("$T logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)", HttpLoggingInterceptor.class)

                    .addStatement("return $T.newBuilder(apiBaseUrl)\n" +
                            "                .addInterceptors(logging)\n" +
                            "                .readTimeout(30000)\n" +
                            "                .retryWhenTimeout(3)\n" +
                            "                .timeBetweenRetry(100)\n" +
                            "                .build()", BaseRetrofitUtils2.class)
                    .returns(Retrofit.class);

            MethodSpec.Builder apiMethod = MethodSpec.methodBuilder("api")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Bean.class)
                    .addStatement("return retrofit().create($T.class)", ClassName.get(apiPackage, "Api"))
                    .returns(ClassName.get(apiPackage, "Api"));

            classBuilder.addMethod(retrofitMethod.build());
            classBuilder.addMethod(apiMethod.build());

            return JavaFile.builder(configPackage, classBuilder.build()).indent("    ").build().toString();
        } else {
            return "";
        }
    }

    private static Multimap<String, String> parseCommand(String cmd) {
        cmd = cmd.replaceAll("\n", "").replaceAll("\\\\", "");
        cmd = cmd.substring(0, cmd.length() - 1);
        String uri = cmd.substring(cmd.lastIndexOf("\'") + 1, cmd.length());

        cmd = StringUtils.substringBefore(cmd, "\'" + uri).replaceAll(" --", " -").replaceAll(" -", "\n -");
        List<String> cmdParts = Splitter.on(" -").trimResults().splitToList(cmd);
        Multimap<String, String> paramsMap = ArrayListMultimap.create();
        cmdParts.forEach(x -> paramsMap.putAll(parseParams(x)));
        paramsMap.putAll(parseUri(uri));
        return paramsMap;
    }

    private static Multimap<String, String> parseUri(String uri) {

        Multimap<String, String> map = ArrayListMultimap.create();
        String baseUrl = uri
                .replace("//", "||")
                .substring(0, uri.replace("//", "||").indexOf("/") + 1)
                .replace("||", "//");
        map.put(BASE_URL, baseUrl);
        String url = "/" + StringUtils.substringAfter(uri, baseUrl);
        if (uri.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
            Map<String, String> queryMap = Splitter.on("&").withKeyValueSeparator("=").split(uri.substring(uri.indexOf("?") + 1, uri.length()));
            queryMap.keySet().forEach(key -> map.put(QUERY, key));
        }
        map.put(REQUEST_URL, url);

        return map;

    }

    private static Multimap<String, String> parseParams(String s) {

        Multimap<String, String> map = ArrayListMultimap.create();
        if (s.startsWith("X") || s.startsWith("request")) {
            map.put(REQUEST_METHOD, s.substring(s.indexOf(32) + 1, s.length()));
        } else if (s.startsWith("H") || s.startsWith("header")) {
            if (!s.contains("Content-Type") && !s.contains("Accept")) {
                map.put(HEADER, s.substring(s.indexOf(32) + 2, s.indexOf(":")));
            }
        } else if (s.startsWith("d") || s.startsWith("data")) {
            map.put(BODY, s.substring(s.indexOf("\'") + 1, s.lastIndexOf("\'")));
        }

        return map;
    }
}
