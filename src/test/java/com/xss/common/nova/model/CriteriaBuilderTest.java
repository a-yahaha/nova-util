package com.xss.common.nova.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CriteriaBuilderTest {
    @Test
    public void test() {
        List<Criteria> criteria = CriteriaBuilder.newBuilder().column("key").le("wyb").build();
        assertEquals(criteria.size(), 1);
        Criteria criterion = criteria.get(0);
        assertEquals(criterion.getColName(), "key");
        assertEquals(criterion.getType(), Criteria.Type.LE);
        assertEquals(criterion.getValue(), "wyb");

        criteria = CriteriaBuilder.newBuilder().column("age").le(40).column("age").gt(20).build();
        assertEquals(criteria.size(), 2);
        criterion = criteria.get(0);
        assertEquals(criterion.getColName(), "age");
        assertEquals(criterion.getType(), Criteria.Type.LE);
        assertEquals(criterion.getValue(), 40);
        criterion = criteria.get(1);
        assertEquals(criterion.getColName(), "age");
        assertEquals(criterion.getType(), Criteria.Type.GT);
        assertEquals(criterion.getValue(), 20);
    }
}
