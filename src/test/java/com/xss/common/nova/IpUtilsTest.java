package com.xss.common.nova;

import com.xss.common.nova.util.BaseIpUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class IpUtilsTest {
    @Test
    public void test() {
        assertFalse(BaseIpUtil.isPublic("127.0.0.1"));
        assertFalse(BaseIpUtil.isPublic("localhost"));
        assertFalse(BaseIpUtil.isPublic("192.168.0.1"));
        assertTrue(BaseIpUtil.isPublic("103.235.46.39"));
    }
}
