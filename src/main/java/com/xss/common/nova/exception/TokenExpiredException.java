package com.xss.common.nova.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
    }

    public TokenExpiredException(String msg) {
        super(msg);
    }
}
