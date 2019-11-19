package com.xss.common.nova;

import com.xss.common.nova.util.BaseJsonUtils;
import com.xss.common.nova.util.BaseReflectUtils;
import lombok.Data;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaseReflectUtilsTest {
    private List<String> list;
    private Set<String> set;
    private List<Bean> beanList;

    @Test
    public void test() throws Exception {
        assertTrue((Boolean) BaseReflectUtils.parse("true", Boolean.class));
        assertEquals(BaseReflectUtils.parse("1", Integer.class), 1);
        assertEquals(BaseReflectUtils.parse("1.1", Double.class) + "", "1.1");
        assertTrue(BaseReflectUtils.parse("2017-01-01", Date.class) != null);

        Bean bean = new Bean();
        bean.setF1("v1");
        bean.setF2(1);
        String beanJson = BaseJsonUtils.writeValue(bean);
        assertEquals(BaseReflectUtils.parse(beanJson, Bean.class), bean);

        list = (List<String>) BaseReflectUtils.parse("[\"hello\", \"world\"]", BaseReflectUtilsTest.class.getDeclaredField("list").getGenericType());
        assertTrue(list.size() == 2);
        assertTrue(list.contains("hello"));

        set = (Set<String>) BaseReflectUtils.parse("[\"hello\", \"world\"]", BaseReflectUtilsTest.class.getDeclaredField("set").getGenericType());
        assertTrue(set.size() == 2);
        assertTrue(set.contains("hello"));

        List<Bean> beans = new ArrayList<>();
        beans.add(bean);
        bean = new Bean();
        bean.setF1("v2");
        bean.setF2(2);
        beans.add(bean);
        beanJson = BaseJsonUtils.writeValue(beans);
        beanList = (List<Bean>) BaseReflectUtils.parse(beanJson, BaseReflectUtilsTest.class.getDeclaredField("beanList").getGenericType());
        assertEquals(beanList, beans);
    }

    @Data
    static class Bean {
        private String f1;
        private Integer f2;
    }
}
