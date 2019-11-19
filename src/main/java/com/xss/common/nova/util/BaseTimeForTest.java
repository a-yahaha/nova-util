package com.xss.common.nova.util;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.Calendar;
import java.util.Date;

public class BaseTimeForTest {
    private Calendar calendar;
    private DateAnswer dateAnswer;
    private SystemTimeAnswer systemTimeAnswer;
    private CalendarAnswer calendarAnswer;

    private BaseTimeForTest(Calendar calendar) {
        this.calendar = calendar;
        this.dateAnswer = new DateAnswer();
        this.systemTimeAnswer = new SystemTimeAnswer();
        this.calendarAnswer = new CalendarAnswer();
        try {
            PowerMockito.whenNew(Date.class).withAnyArguments().thenAnswer(dateAnswer);
            PowerMockito.mockStatic(System.class, systemTimeAnswer);
            PowerMockito.mockStatic(Calendar.class, calendarAnswer);
        } catch (Exception e) {
            throw new RuntimeException("初始化BaseTimeForTest异常", e);
        }
    }

    public static BaseTimeForTest instance(Long timeInMillis) {
        Calendar now = Calendar.getInstance();
        if (timeInMillis != null) {
            now.setTimeInMillis(timeInMillis);
        }
        return new BaseTimeForTest(now);
    }

    public void setTime(long timeInMillis) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        calendar.setTimeInMillis(timeInMillis);
    }

    public void recover() {
        calendar = null;
    }

    private class DateAnswer implements Answer<Date> {
        @Override
        public Date answer(InvocationOnMock invocationOnMock) throws Throwable {
            if (invocationOnMock.getArguments() == null || invocationOnMock.getArguments().length == 0) {
                if (calendar != null) {
                    return new Date(calendar.getTimeInMillis());
                } else {
                    return new Date();
                }
            } else {
                return new Date((long) invocationOnMock.getArguments()[0]);
            }
        }
    }

    private class SystemTimeAnswer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            if ("currentTimeMillis".equals(invocationOnMock.getMethod().getName())) {
                if (calendar != null) {
                    return calendar.getTimeInMillis();
                } else {
                    return System.currentTimeMillis();
                }
            } else {
                return invocationOnMock.callRealMethod();
            }
        }
    }

    private class CalendarAnswer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            if ("getInstance".equals(invocationOnMock.getMethod().getName())) {
                if (calendar != null) {
                    return calendar.clone();
                } else {
                    return Calendar.getInstance();
                }
            } else {
                return invocationOnMock.callRealMethod();
            }
        }
    }
}
