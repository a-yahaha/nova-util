package com.xss.common.nova.model;

import lombok.Data;

@Data
public class PageRequest {
    private Integer page;
    private Integer pageSize;
    private String orderedCol;
    private Order order;

    public PageRequest() {
        this.order = Order.DESC;
    }

    protected PageRequest(Integer page, Integer pageSize) {
        this.page = page;
        this.pageSize = pageSize;
        this.order = Order.DESC;
    }

    public static PageRequest of(Integer page, Integer pageSize) {
        return new PageRequest(page, pageSize);
    }

    public PageRequest orderBy(String orderedCol) {
        this.orderedCol = orderedCol;
        return this;
    }

    public PageRequest orderBy(String orderedCol, Order order) {
        this.orderedCol = orderedCol;
        this.order = order;
        return this;
    }


    public enum Order {
        ASC,
        DESC;
    }
}
