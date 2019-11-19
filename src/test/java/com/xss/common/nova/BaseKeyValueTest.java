package com.xss.common.nova;

import com.xss.common.nova.util.BaseKeyValue;
import jodd.util.ThreadUtil;
import org.junit.Assert;
import org.junit.Test;

public class BaseKeyValueTest {
    @Test
    public void test() {
        BaseKeyValue.put("key", "value");
        new Thread(() -> {
            Assert.assertEquals(BaseKeyValue.get("key"), "value");
        }).start();
    }

    @Test
    public void testMultiThread() {
        new Thread(() -> {
            BaseKeyValue.put("key", "value");
            Assert.assertEquals(BaseKeyValue.get("key"), "value");
            ThreadUtil.sleep(200);
            BaseKeyValue.put("key", "changed");
            BaseKeyValue.clear();
        }).start();

        new Thread(() -> {
            BaseKeyValue.put("key", "value2");
            ThreadUtil.sleep(1000);
            Assert.assertEquals(BaseKeyValue.get("key"), "value2");
        }).start();
    }
}
