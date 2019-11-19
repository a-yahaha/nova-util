package com.xss.common.nova;

import com.xss.common.nova.util.BaseEnumUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class BaseEnumUtilsTest {
    @Test
    public void test() {
        TestEnum t = BaseEnumUtils.getEnum(TestEnum.class, "one", TestEnum.DEFAUL);
        assertNotNull(t);
        assertTrue(t == TestEnum.ONE);

        t = BaseEnumUtils.getEnum(TestEnum.class, "ONE");
        assertNotNull(t);
        assertTrue(t == TestEnum.ONE);

        t = BaseEnumUtils.getEnum(TestEnum.class, "hello");
        assertNull(t);
    }

    enum TestEnum {
        ONE,
        TWO,
        DEFAUL
    }
}
