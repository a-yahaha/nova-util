package com.xss.common.nova.model;

import lombok.Data;

@Data
public class IdCardInfo {
    private String province;
    private String city;
    private String county;
    private String birthDate;
    private Integer age;
    private boolean isMale;
}
