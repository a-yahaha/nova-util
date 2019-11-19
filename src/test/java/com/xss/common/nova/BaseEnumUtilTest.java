package com.xss.common.nova;

import com.xss.common.nova.util.BaseEnumUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class BaseEnumUtilTest {
    @Test
    public void test() {
        TestEnum t = BaseEnumUtil.getEnum(TestEnum.class, "one", TestEnum.DEFAUL);
        assertNotNull(t);
        assertTrue(t == TestEnum.ONE);

        t = BaseEnumUtil.getEnum(TestEnum.class, "ONE");
        assertNotNull(t);
        assertTrue(t == TestEnum.ONE);

        t = BaseEnumUtil.getEnum(TestEnum.class, "hello");
        assertNull(t);
    }

    enum TestEnum {
        ONE,
        TWO,
        DEFAUL
    }
}
