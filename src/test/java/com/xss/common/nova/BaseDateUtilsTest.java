package com.xss.common.nova;

import com.xss.common.nova.util.BaseDateUtils;
import com.xss.common.nova.util.BaseMathUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

@Slf4j
public class BaseDateUtilsTest {
    @Test
    public void test() {
        Date date =  BaseDateUtils.parseDate("1977-11-11", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
        Assert.assertEquals(BaseDateUtils.toDateFormat(date), "1977-11-11");
        Date date2 = BaseDateUtils.add(date, Calendar.MONTH,  -1);
        Assert.assertEquals(BaseDateUtils.toTimeFormat(date2), "1977-10-11 00:00:00");
        Assert.assertTrue(BaseDateUtils.isSameYear(date, date2));
        Date date3 = BaseDateUtils.getMonthEnd(date2);
        Assert.assertEquals(BaseDateUtils.toDateFormat(date3), "1977-10-31");

        String s = "2000-01-01 00:00:00";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyy-MM-dd HH:mm:ss"));

        s = "2000-01-01";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyy-MM-dd"));

        s = "2000-1-01";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyy-M-dd"));

        s = "2000-01";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyy-MM"));

        s = "2000-1";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyy-M"));

        s = "20000101000000000";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyyMMddHHmmssSSS"));

        s = "20000101000000";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyyMMddHHmmss"));

        s = "2000101000000";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyyMddHHmmss"));

        s = "20000101";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyyMMdd"));

        s = "200001";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyyMM"));

        s = "20001";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyyM"));

        s = "2000/01/01 00:00:00";
        date = BaseDateUtils.parseDate(s);
        Assert.assertEquals(s, BaseDateUtils.format(date, "yyyy/MM/dd HH:mm:ss"));
    }

    @Test
    public void testCompare() {
        Date date1 = BaseDateUtils.parseDate("2000-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date date2 = BaseDateUtils.parseDate("2000-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(BaseDateUtils.compareDay(date1, date2) == 1);

        date1 = BaseDateUtils.parseDate("2000-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
        date2 = BaseDateUtils.parseDate("2000-01-01 23:59:59", "yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(BaseDateUtils.compareDay(date1, date2) == 0);

        date1 = BaseDateUtils.parseDate("2000-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss");
        date2 = BaseDateUtils.parseDate("2000-01-01 23:59:59", "yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(BaseDateUtils.compareDay(date1, date2) == 1);

        date1 = BaseDateUtils.parseDate("2000-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
        date2 = BaseDateUtils.parseDate("2000-01-01 23:59:59", "yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(BaseDateUtils.compareDay(date1, date2) == 31);

        date1 = BaseDateUtils.parseDate("2017-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
        date2 = BaseDateUtils.parseDate("2000-01-01 23:59:59", "yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(BaseDateUtils.compareYear(date1, date2) == 17);
        Assert.assertTrue(BaseDateUtils.compareMonth(date1, date2) == 205);

        date2 = BaseDateUtils.parseDate("1999-12-01 23:59:59", "yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(BaseDateUtils.compareYear(date1, date2) == 18);
    }

    @Test
    public void testAdd() {
        Date date =  BaseDateUtils.parseDate("1977-11-11", "yyyy-MM-dd");
        Date dateTest = BaseDateUtils.add(date, "1s");
        Assert.assertEquals(dateTest.getTime(), date.getTime() + 1000);

        dateTest = BaseDateUtils.add(date, "-1s");
        Assert.assertEquals(dateTest.getTime(), date.getTime() - 1000);

        dateTest = BaseDateUtils.add(date, "1d");
        Assert.assertEquals(dateTest.getTime(), date.getTime() + 24*3600*1000);

        dateTest = BaseDateUtils.add(date, "1 d");
        Assert.assertEquals(dateTest.getTime(), date.getTime() + 24*3600*1000);

        dateTest = BaseDateUtils.add(date, " 1  d ");
        Assert.assertEquals(dateTest.getTime(), date.getTime() + 24*3600*1000);

        try {
            BaseDateUtils.add(date, "1dd");
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("不符合格式"));
        }

        dateTest = BaseDateUtils.add(date, "1d1h");
        Assert.assertEquals(dateTest.getTime(), date.getTime() + 24*3600*1000 + 3600*1000);

        dateTest = BaseDateUtils.add(date, "1d1h", BaseMathUtils.multiplyTime("2h", 2));
        Assert.assertEquals(dateTest.getTime(), date.getTime() + 24*3600*1000 + 5*3600*1000);
    }
}
