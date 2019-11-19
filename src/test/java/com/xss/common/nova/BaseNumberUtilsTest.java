package com.xss.common.nova;

import com.xss.common.nova.util.BaseNumberUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class BaseNumberUtilsTest {
    @Test
    public void test() {
        int num = 12345;
        assertEquals("123", BaseNumberUtils.numToString(num, 3, ' '));
        assertEquals("012345", BaseNumberUtils.numToString(num, 6, '0'));
    }
}
