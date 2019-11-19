package com.xss.common.nova;

import com.xss.common.nova.util.BaseJsonUtil;
import com.xss.common.nova.util.BaseReflectUtil;
import lombok.Data;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaseReflectUtilTest {
    private List<String> list;
    private Set<String> set;
    private List<Bean> beanList;

    @Test
    public void test() throws Exception {
        assertTrue((Boolean) BaseReflectUtil.parse("true", Boolean.class));
        assertEquals(BaseReflectUtil.parse("1", Integer.class), 1);
        assertEquals(BaseReflectUtil.parse("1.1", Double.class) + "", "1.1");
        assertTrue(BaseReflectUtil.parse("2017-01-01", Date.class) != null);

        Bean bean = new Bean();
        bean.setF1("v1");
        bean.setF2(1);
        String beanJson = BaseJsonUtil.writeValue(bean);
        assertEquals(BaseReflectUtil.parse(beanJson, Bean.class), bean);

        list = (List<String>) BaseReflectUtil.parse("[\"hello\", \"world\"]", BaseReflectUtilTest.class.getDeclaredField("list").getGenericType());
        assertTrue(list.size() == 2);
        assertTrue(list.contains("hello"));

        set = (Set<String>) BaseReflectUtil.parse("[\"hello\", \"world\"]", BaseReflectUtilTest.class.getDeclaredField("set").getGenericType());
        assertTrue(set.size() == 2);
        assertTrue(set.contains("hello"));

        List<Bean> beans = new ArrayList<>();
        beans.add(bean);
        bean = new Bean();
        bean.setF1("v2");
        bean.setF2(2);
        beans.add(bean);
        beanJson = BaseJsonUtil.writeValue(beans);
        beanList = (List<Bean>) BaseReflectUtil.parse(beanJson, BaseReflectUtilTest.class.getDeclaredField("beanList").getGenericType());
        assertEquals(beanList, beans);
    }

    @Data
    static class Bean {
        private String f1;
        private Integer f2;
    }
}
