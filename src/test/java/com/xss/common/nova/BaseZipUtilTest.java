package com.xss.common.nova;

import com.xss.common.nova.util.BaseZipUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseZipUtilTest {
    @Test
    public void test() {
        String text = RandomStringUtils.randomAlphabetic(100) + "王师父";
        byte[] data = text.getBytes();

        String zipped = BaseZipUtil.zip(text);
        assertEquals(text, BaseZipUtil.unzip(zipped));

        byte[] zipped2 = BaseZipUtil.zip(data);
        assertEquals(text, new String(BaseZipUtil.unzip(zipped2)));
    }
}
