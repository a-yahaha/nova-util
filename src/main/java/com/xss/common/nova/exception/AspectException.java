package com.xss.common.nova.exception;

import lombok.Data;

@Data
public class AspectException extends RuntimeException {
    private String errorKey;
    private String[] args;
    private boolean logError = true;

    public AspectException(String errorKey, boolean logAsError, String... args) {
        this.errorKey = errorKey;
        this.args = args;
        this.logError = logAsError;
    }

    public static AspectException fromKey(String key, String... args) {
        return fromKey(key, true, args);
    }

    public static AspectException fromKey(String key, boolean logAsError, String... args) {
        return new AspectException(key, logAsError, args);
    }
}
