package com.xss.common.nova;

import com.xss.common.nova.util.BaseDateUtils;
import com.xss.common.nova.util.BaseTimeForTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest({BaseDateUtils.class})
public class BaseTimeForTestTest {
    @Test
    public void test() throws Exception {
        String start = "2001-01-01 12:00:00";
        long now = BaseDateUtils.fromTimeFormat(start).getTime();
        long oneDay = 24 * 3600 * 1000L;

        log.info("add one day ====================");
        BaseTimeForTest crtTime = BaseTimeForTest.instance(now + oneDay);
        assertEquals("2001-01-02 00:00:00", BaseDateUtils.toTimeFormat(BaseDateUtils.getDayStart()));
        assertEquals("2001-01-02 12:00:00", BaseDateUtils.toTimeFormat(new Date()));
        assertEquals("2001-01-02 12:00:00", BaseDateUtils.toTimeFormat(new Date(System.currentTimeMillis())));
        assertEquals("2001-01-02 12:00:00", BaseDateUtils.toTimeFormat(Calendar.getInstance().getTime()));

        log.info("add two day ====================");
        crtTime.setTime(now + oneDay * 2);
        assertEquals("2001-01-03 00:00:00", BaseDateUtils.toTimeFormat(BaseDateUtils.getDayStart()));
        assertEquals("2001-01-03 12:00:00", BaseDateUtils.toTimeFormat(new Date()));
        assertEquals("2001-01-03 12:00:00", BaseDateUtils.toTimeFormat(new Date(System.currentTimeMillis())));

        log.info("recover ====================");
        crtTime.recover();
        assertEquals(BaseDateUtils.toDateFormat(Calendar.getInstance().getTime()), BaseDateUtils.toDateFormat(BaseDateUtils.getDayStart()));
    }
}
