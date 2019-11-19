package com.xss.common.nova.model;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CriteriaBuilder {
    private List<Criteria> criteria = new ArrayList<>();

    private CriteriaBuilder() {

    }

    public static CriteriaBuilder newBuilder() {
        return new CriteriaBuilder();
    }

    public CriteriaBuilder criteria(Criteria criterion) {
        criteria.add(criterion);
        return this;
    }

    public CriteriaBuilder column(String colName) {
        criteria.add(Criteria.column(colName));
        return this;
    }

    public CriteriaBuilder or(String colName) {
        criteria.add(Criteria.or(colName));
        return this;
    }

    public CriteriaBuilder eq(Object value) {
        criteria.get(criteria.size() - 1).eq(value);
        return this;
    }

    public CriteriaBuilder ne(Object value) {
        criteria.get(criteria.size() - 1).ne(value);
        return this;
    }

    public CriteriaBuilder gt(Object value) {
        criteria.get(criteria.size() - 1).gt(value);
        return this;
    }

    public CriteriaBuilder ge(Object value) {
        criteria.get(criteria.size() - 1).ge(value);
        return this;
    }

    public CriteriaBuilder lt(Object value) {
        criteria.get(criteria.size() - 1).lt(value);
        return this;
    }

    public CriteriaBuilder le(Object value) {
        criteria.get(criteria.size() - 1).le(value);
        return this;
    }

    public CriteriaBuilder in(List value) {
        criteria.get(criteria.size() - 1).in(value);
        return this;
    }

    public CriteriaBuilder nin(List value) {
        criteria.get(criteria.size() - 1).nin(value);
        return this;
    }

    public CriteriaBuilder like(Object value) {
        criteria.get(criteria.size() - 1).like(value);
        return this;
    }

    public CriteriaBuilder isNull() {
        criteria.get(criteria.size() - 1).isNull();
        return this;
    }

    public CriteriaBuilder isNotNull() {
        criteria.get(criteria.size() - 1).isNotNull();
        return this;
    }

    public List<Criteria> build() {
        criteria.forEach(criterion -> {
            checkCriteria(criterion);
        });
        return criteria;
    }

    public Criteria buildCombined() {
        criteria.forEach(criterion -> checkCriteria(criterion));
        return new CombinedCriteria(criteria);
    }

    private void checkCriteria(Criteria criterion) {
        if(criterion instanceof CombinedCriteria) {
            ((CombinedCriteria) criterion).getCriteria().forEach(criteria -> checkCriteria(criteria));
        } else {
            Preconditions.checkArgument(StringUtils.isNotBlank(criterion.getColName()), "criteria中colName不能为空");
            Preconditions.checkArgument(criterion.getType() != null, "criteria中type不能为空");
            Preconditions.checkArgument(criterion.getType() == Criteria.Type.IS_NULL ||
                    criterion.getType() == Criteria.Type.IS_NOT_NULL || criterion.getValue() != null, "criteria中value不能为空");
        }
    }
}
