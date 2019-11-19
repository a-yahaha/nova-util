package com.xss.common.nova.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JwtToken {
    private String token;
    @JsonProperty("exp")
    private Long expireAt;
}
