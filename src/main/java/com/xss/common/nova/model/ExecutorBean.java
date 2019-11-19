package com.xss.common.nova.model;

import com.google.common.collect.Table;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.Map;

@Data
public class ExecutorBean {
    private Table<String, String, Type> paramsTable;
    private Map<String, String> urlInfoMap;
    private Type returnType;
    private String methodName;
}
