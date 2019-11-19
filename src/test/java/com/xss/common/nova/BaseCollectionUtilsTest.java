package com.xss.common.nova;

import com.xss.common.nova.util.BaseCollectionUtils;
import lombok.Data;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaseCollectionUtilsTest {
    @Test
    public void test() {
        Test1 test1 = new Test1("wyb", 24);
        Test1 test11 = new Test1("wyb", 25);
        Test1 test12 = new Test1("wyb", 24);
        Test1 test13 = new Test1("wyb", 26);
        List<Test1> list = Arrays.asList(test1, test11);
        assertTrue(BaseCollectionUtils.contains(list, test12));
        assertFalse(BaseCollectionUtils.contains(list, test13));
        assertTrue(BaseCollectionUtils.containsIgnoreCase(Arrays.asList("wyb", "lll"), "Wyb"));
        assertFalse(BaseCollectionUtils.containsIgnoreCase(Arrays.asList("wyb", "lll"), "Wybb"));
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
