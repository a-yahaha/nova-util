package com.xss.common.nova.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 与BaseAspectUtils协同工作.
 * 在被 使用了BaseAspectUtils的aspect 拦截的方法上加上此annotation,此annotation中的信息会打印在log中
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogInfo {
    String value() default "";

    String[] params() default {};

    boolean replace() default true;

    String errorKey() default "";
    String[] errorParams() default {};
    boolean logError() default true;
}
