package com.xss.common.nova.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ComplexUser {
    private String name;
    private Integer age;
    private Gender gender;
    private Address addr;
    private List<Address> otherAddr;
    private List<String> test = new ArrayList<>();
    private Map<String, String> tags = new HashMap<>();
}
