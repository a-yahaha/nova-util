package com.xss.common.nova;

import com.xss.common.nova.util.BaseIpUtils;
import org.junit.Test;
import static org.junit.Assert.*;
public class IpUtilsTest {
    @Test
    public void test() {
        assertFalse(BaseIpUtils.isPublic("127.0.0.1"));
        assertFalse(BaseIpUtils.isPublic("localhost"));
        assertFalse(BaseIpUtils.isPublic("192.168.0.1"));
        assertTrue(BaseIpUtils.isPublic("103.235.46.39"));
    }
}
