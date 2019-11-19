package com.xss.common.nova.util;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

@Slf4j
public class BaseMachineUtil {
    public static String getHostName() {
        String hostName = null;
        try {
            InetAddress address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (Throwable e) {

        }
        if (StringUtils.isNotBlank(hostName) && !"localhost".equalsIgnoreCase(hostName)) {
            return hostName;
        }

        try {
            hostName = BaseProcessUtil.run("hostname");
        } catch (Exception e) {
        }
        if (StringUtils.isNotBlank(hostName) && !"localhost".equalsIgnoreCase(hostName)) {
            return hostName;
        }

        return null;
    }

    public static String getIpAddress() {
        String ip = null;
        try {
            InetAddress address = InetAddress.getLocalHost();
            ip = address.getHostAddress();
        } catch (Throwable e) {

        }
        if (StringUtils.isNotBlank(ip) && !"localhost".equalsIgnoreCase(ip) && !"127.0.0.1".equals(ip)) {
            return ip;
        }

        return null;
    }
}
