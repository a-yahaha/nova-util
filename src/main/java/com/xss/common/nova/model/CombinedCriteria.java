package com.xss.common.nova.model;

import com.google.common.base.Preconditions;
import com.xss.common.nova.util.BaseCollectionUtil;
import lombok.Getter;

import java.util.List;

@Getter
public class CombinedCriteria extends Criteria {
    private List<Criteria> criteria;

    protected CombinedCriteria(List<Criteria> criteria) {
        super(null);
        this.criteria = criteria;
        Preconditions.checkArgument(BaseCollectionUtil.isNotEmpty(criteria), "CombinedCriteria构造函数不允许传入null或空Criteria");
    }

    @Override
    public boolean isOr() {
        return criteria.get(0).isOr();
    }
}
