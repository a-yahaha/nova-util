package com.xss.common.nova.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class JdbcResult {
    private String sql;
    private List<Object[]> paramsList;

    public JdbcResult(String sql, Object[] params) {
        this.sql = sql;
        paramsList = new ArrayList<>();
        paramsList.add(params);
    }

    public JdbcResult(String sql, List<Object[]> paramsList) {
        this.sql = sql;
        this.paramsList = paramsList;
    }

    public String getSql() {
        return sql;
    }

    public Object[] getParams() {
        return paramsList.get(0);
    }

    public List<Object[]> getBatchParams() {
        return paramsList;
    }
}
