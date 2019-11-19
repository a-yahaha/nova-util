package com.xss.common.nova.exception;

import lombok.Data;

@Data
public class IpInvalidException extends RuntimeException {
    private String ip;

    public IpInvalidException(String ip) {
        this.ip = ip;
    }
}
