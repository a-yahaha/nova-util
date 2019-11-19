package com.xss.common.nova.util;

import com.auth0.jwt.internal.org.apache.commons.io.IOUtils;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Slf4j
public class BaseWebUtil {
    private static Set<String> IGNORED_HEADERS = ImmutableSet.of("accept-charset", "accept-encoding", "accept-language",
            "accept-ranges", "age", "allow", "cache-control", "connection", "content-encoding", "content-language",
            "content-length", "content-location", "content-md5", "content-range", "date", "dav", "depth", "destination",
            "etag", "expect", "expires", "from", "host", "if", "if-match", "if-modified-since", "if-none-match",
            "if-range", "if-unmodified-since", "last-modified", "location", "lock-token", "max-forwards", "overwrite",
            "pragma", "proxy-authenticate", "proxy-authorization", "range", "referer", "retry-after", "server", "status-uri",
            "te", "timeout", "trailer", "transfer-encoding", "upgrade", "user-agent", "vary", "via", "warning", "www-authenticate");

    public static Method getMethod(RequestMappingHandlerMapping mapping, ServletRequest servletRequest) {
        if (mapping == null || !(servletRequest instanceof HttpServletRequest)) {
            return null;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        try {
            HandlerExecutionChain chain = mapping.getHandler(request);
            if (chain == null) {
                return null;
            }

            Object handlerMethod = chain.getHandler();
            if (handlerMethod == null || !(handlerMethod instanceof HandlerMethod)) {
                return null;
            }

            return ((HandlerMethod) handlerMethod).getMethod();
        } catch (Throwable e) {
            log.error("获取handler method异常", e);
            return null;
        }
    }

    public static String requestKey(HttpServletRequest request) throws IOException {
        String sep = ":";
        if (request == null) {
            return null;
        }

        //基本的url
        String method = request.getMethod();
        StringBuilder buf = new StringBuilder(method);
        buf.append(sep).append(request.getRequestURL());
        if (StringUtils.isNotBlank(request.getQueryString())) {
            buf.append(sep).append(request.getQueryString());
        }

        //http头
        List<String> headerNames = new ArrayList<>();
        Enumeration enumeration = request.getHeaderNames();
        while (enumeration != null && enumeration.hasMoreElements()) {
            String header = (String) enumeration.nextElement();
            if (!IGNORED_HEADERS.contains(header.toLowerCase())) {
                headerNames.add(header);
            }
        }
        if (!CollectionUtils.isEmpty(headerNames)) {
            buf.append(sep);
        }
        IntStream.range(0, headerNames.size()).forEach(index -> {
            String header = headerNames.get(index);
            if (index != 0) {
                buf.append(";");
            }
            buf.append(header).append("=").append(request.getHeader(header));
        });

        //请求体
        if(method.equalsIgnoreCase("put") || method.equalsIgnoreCase("post") || method.equalsIgnoreCase("patch")) {
            String body = IOUtils.toString(request.getReader());
            if (StringUtils.isNotBlank(body)) {
                buf.append(sep).append(body);
            }
        }

        return buf.toString().replaceAll("\\r?\\n", "");
    }
}
