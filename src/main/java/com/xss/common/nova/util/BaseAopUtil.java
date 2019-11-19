package com.xss.common.nova.util;

import org.springframework.aop.framework.AdvisedSupport;

import java.lang.reflect.Field;

public class BaseAopUtil {
    public static Object getObjectFromCglibProxy(Object proxy) {
        try {
            Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
            h.setAccessible(true);
            Object dynamicAdvisedInterceptor = h.get(proxy);
            Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            return ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        } catch (Throwable e) {
            return null;
        }
    }
}
