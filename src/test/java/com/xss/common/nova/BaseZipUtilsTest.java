package com.xss.common.nova;

import com.xss.common.nova.util.BaseZipUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseZipUtilsTest {
    @Test
    public void test() {
        String text = RandomStringUtils.randomAlphabetic(100) + "王师父";
        byte[] data = text.getBytes();

        String zipped = BaseZipUtils.zip(text);
        assertEquals(text, BaseZipUtils.unzip(zipped));

        byte[] zipped2 = BaseZipUtils.zip(data);
        assertEquals(text, new String(BaseZipUtils.unzip(zipped2)));
    }
}
