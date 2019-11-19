package com.xss.common.nova;

import com.xss.common.nova.util.BaseCollectionUtil;
import lombok.Data;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaseCollectionUtilTest {
    @Test
    public void test() {
        Test1 test1 = new Test1("wyb", 24);
        Test1 test11 = new Test1("wyb", 25);
        Test1 test12 = new Test1("wyb", 24);
        Test1 test13 = new Test1("wyb", 26);
        List<Test1> list = Arrays.asList(test1, test11);
        assertTrue(BaseCollectionUtil.contains(list, test12));
        assertFalse(BaseCollectionUtil.contains(list, test13));
        assertTrue(BaseCollectionUtil.containsIgnoreCase(Arrays.asList("wyb", "lll"), "Wyb"));
        assertFalse(BaseCollectionUtil.containsIgnoreCase(Arrays.asList("wyb", "lll"), "Wybb"));
    }

    @Data
    static class Test1 {
        private String f1;
        private Integer f2;

        public Test1(String f1, Integer f2) {
            this.f1 = f1;
            this.f2 = f2;
        }
    }
}
