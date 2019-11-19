package com.xss.common.nova;

import com.xss.common.nova.util.BaseNumberUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseNumberUtilTest {
    @Test
    public void test() {
        int num = 12345;
        assertEquals("123", BaseNumberUtil.numToString(num, 3, ' '));
        assertEquals("012345", BaseNumberUtil.numToString(num, 6, '0'));
    }
}
