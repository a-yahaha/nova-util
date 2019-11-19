package com.xss.common.nova.model;

import lombok.Getter;

import java.util.List;

@Getter
public class Criteria {
    private boolean or = false;
    private String colName;
    private Type type;
    private Object value;

    protected Criteria(String colName) {
        this.colName = colName;
    }

    protected Criteria(String colName, boolean or) {
        this.colName = colName;
        this.or = or;
    }

    public static Criteria column(String colName) {
        return new Criteria(colName);
    }

    public static Criteria or(String colName) {
        return new Criteria(colName, true);
    }

    public Criteria eq(Object value) {
        this.type = Type.EQ;
        this.value = value;
        return this;
    }

    public Criteria ne(Object value) {
        this.type = Type.NE;
        this.value = value;
        return this;
    }

    public Criteria gt(Object value) {
        this.type = Type.GT;
        this.value = value;
        return this;
    }

    public Criteria ge(Object value) {
        this.type = Type.GE;
        this.value = value;
        return this;
    }

    public Criteria lt(Object value) {
        this.type = Type.LT;
        this.value = value;
        return this;
    }

    public Criteria le(Object value) {
        this.type = Type.LE;
        this.value = value;
        return this;
    }

    public Criteria in(List value) {
        this.type = Type.IN;
        this.value = value;
        return this;
    }

    public Criteria nin(List value) {
        this.type = Type.NIN;
        this.value = value;
        return this;
    }

    public Criteria like(Object value) {
        this.type = Type.LIKE;
        this.value = value;
        return this;
    }

    public Criteria isNull() {
        this.type = Type.IS_NULL;
        return this;
    }

    public Criteria isNotNull() {
        this.type = Type.IS_NOT_NULL;
        return this;
    }

    public enum Type {
        EQ, //等于
        NE, //不等于
        GT, //大于
        GE, //大于或等于
        LT, //小于
        LE, //小于或等于
        IN, //在范围内
        NIN, //不在范围内
        LIKE,// 模糊匹配
        IS_NULL,
        IS_NOT_NULL
    }
}
