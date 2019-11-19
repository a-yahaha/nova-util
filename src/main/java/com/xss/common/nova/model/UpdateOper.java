package com.xss.common.nova.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class UpdateOper {
    private Type type;
    private Object value;

    @Getter
    public enum Type {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }
}
