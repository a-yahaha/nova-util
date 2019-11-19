package com.xss.common.nova.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.net.URL;
import java.util.Properties;

@Slf4j
public class BasePropertiesUtil {
    public static Properties load(String fileName) {
        Properties props = null;
        try {
            URL url = BasePropertiesUtil.class.getClassLoader().getResource(fileName);
            if (url != null) {
                props = PropertiesLoaderUtils.loadProperties(new EncodedResource(new UrlResource(url), "utf-8"));
            }
        } catch (Throwable e) {
            log.error("加载配置文件[{}]异常: {}", fileName, ExceptionUtils.getStackTrace(e));
        }

        if (props == null) {
            log.error("找不到配置文件:" + fileName);
        }
        return props;
    }
}
