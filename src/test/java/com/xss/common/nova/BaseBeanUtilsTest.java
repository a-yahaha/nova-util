package com.xss.common.nova;

import com.xss.common.nova.util.BaseBeanUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

@Slf4j
public class BaseBeanUtilsTest {
    @Test
    public void testCopyProperties() {
        Bean1 bean1 = new Bean1();
        bean1.setF1("1");
        Bean2 bean2 = new Bean2();
        bean2.setF2("2");
        Bean3 bean3 = new Bean3();
        bean3.setF1("31");
        bean3.setF2("32");
        BaseBeanUtils.copyProperties(bean1, bean2);
        Assert.assertEquals(bean2.getF2(), "2");
        BaseBeanUtils.copyProperties(bean1, bean3);
        Assert.assertEquals(bean3.getF1(), "1");
        BaseBeanUtils.copyProperties(bean3, bean2);
        Assert.assertEquals(bean2.getF2(), "32");
        bean3.setF2(null);
        BaseBeanUtils.copyProperties(bean3, bean2);
        Assert.assertNull(bean2.getF2());
        bean2.setF2("new2");
        BaseBeanUtils.copyNoneNullProperties(bean3, bean2);
        Assert.assertEquals(bean2.getF2(), "new2");
    }

    @Test
    public void testClone() {
        Outer outer = new Outer();
        outer.setF1("1");
        Inner inner = new Inner();
        inner.setF11("11");
        outer.setInner(inner);

        Outer outerCopy = BaseBeanUtils.shallowClone(outer);
        Assert.assertTrue(outer != outerCopy);
        Assert.assertTrue(outer.getF1() == outerCopy.getF1());
        Assert.assertTrue(outer.getInner() == outerCopy.getInner());

        outerCopy = BaseBeanUtils.deepClone(outer);
        Assert.assertTrue(outer != outerCopy);
        Assert.assertEquals(outer.getF1(), outerCopy.getF1());
        Assert.assertTrue(outer.getInner() != outerCopy.getInner());
        Assert.assertEquals(outer.getInner().getF11(), outerCopy.getInner().getF11());
    }

    @Test
    public void testBeanToMap() {
        Bean3 bean3 = new Bean3();
        bean3.setF1("1");
        bean3.setF2("2");
        Map<String, Object> map = BaseBeanUtils.beanToMap(bean3);
        Assert.assertEquals(map.get("f1"), "1");
        Assert.assertEquals(map.get("f2"), "2");
        Assert.assertEquals(map.size(), 2);

        bean3.setF2(null);
        map = BaseBeanUtils.beanToMap(bean3);
        Assert.assertEquals(map.get("f1"), "1");
        Assert.assertEquals(map.get("f2"), null);
        Assert.assertEquals(map.size(), 2);

        map = BaseBeanUtils.beanToMapNonNull(bean3);
        Assert.assertEquals(map.get("f1"), "1");
        Assert.assertEquals(map.size(), 1);
    }

    @Test
    public void testMap() {
        Bean3 bean3 = new Bean3();
        bean3.setF1("1");
        bean3.setF2("2");
        Bean1 bean1 = BaseBeanUtils.convert(bean3, Bean1.class);
        Assert.assertEquals("1", bean1.getF1());

        Bean2 bean2 = new Bean2();
        bean2.setF2("22");
        bean3 = BaseBeanUtils.convert(bean2, Bean3.class);
        Assert.assertEquals(bean3.getF2(), "22");
    }


    @Data
    public static class Bean1 {
        String f1;
    }

    @Data
    public static class Bean2 {
        String f2;
    }

    @Data
    public static class Bean3 {
        String f1;
        String f2;
    }

    @Data
    public static class Outer {
        private String f1;
        private Inner inner;
    }

    @Data
    public static class Inner {
        private String f11;
    }
}
