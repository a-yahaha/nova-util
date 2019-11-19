package com.xss.common.nova.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class PageResponse<T> {
    private Integer total;
    private List<T> datas = Collections.emptyList();

    public PageResponse(List<T> datas) {
        if (datas == null) {
            datas = Collections.emptyList();
        }
        this.datas = datas;
    }

    public PageResponse(Integer total, List<T> datas) {
        if (datas == null) {
            datas = Collections.emptyList();
        }
        this.datas = datas;
        this.total = total;
    }
}
