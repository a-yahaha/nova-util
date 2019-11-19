package com.xss.common.nova.exception;

import lombok.Data;

@Data
public class RetrofitException extends RuntimeException {
    private Integer code;
    private String message;

    public RetrofitException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
