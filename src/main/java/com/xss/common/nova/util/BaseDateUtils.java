package com.xss.common.nova.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 时间相关工具类
 */
@Slf4j
public class BaseDateUtils {
    public static final String COMPACT_PTN = "yyyyMMddHHmmss";
    public static final String STANDARD_PTN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String TIME_PTN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PTN = "yyyy-MM-dd";

    public static final TimeZone CHINA_TIMEZONE = TimeZone.getTimeZone("GMT+8");
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd", CHINA_TIMEZONE);
    private static final FastDateFormat TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", CHINA_TIMEZONE);
    private static final FastDateFormat STANDARD_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ", CHINA_TIMEZONE);
    private static final Map<String, FastDateFormat> defaultFormats = ImmutableMap.<String, FastDateFormat>builder()
            .put("yyyy-MM-dd", DATE_FORMAT)
            .put("yyyy-MM-dd HH:mm:ss", TIME_FORMAT)
            .put("yyyy-MM-dd'T'HH:mm:ss.SSSZ", STANDARD_FORMAT)
            .put("yyyyMMdd", FastDateFormat.getInstance("yyyyMMdd", CHINA_TIMEZONE))
            .put("yyyyMMddHHmmss", FastDateFormat.getInstance("yyyyMMddHHmmss", CHINA_TIMEZONE))
            .put("yyyyMMddHHmmssSSS", FastDateFormat.getInstance("yyyyMMddHHmmssSSS", CHINA_TIMEZONE))
            .put("yyyy-M-dd", FastDateFormat.getInstance("yyyy-M-dd", CHINA_TIMEZONE))
            .put("yyyy-MM", FastDateFormat.getInstance("yyyy-MM", CHINA_TIMEZONE))
            .put("yyyy-M", FastDateFormat.getInstance("yyyy-M", CHINA_TIMEZONE))
            .put("yyyyMddHHmmss", FastDateFormat.getInstance("yyyyMddHHmmss", CHINA_TIMEZONE))
            .put("yyyyMM", FastDateFormat.getInstance("yyyyMM", CHINA_TIMEZONE))
            .put("yyyyM", FastDateFormat.getInstance("yyyyM", CHINA_TIMEZONE))
            .put("yyyy/MM/dd", FastDateFormat.getInstance("yyyy/MM/dd", CHINA_TIMEZONE))
            .put("yyyy/MM/dd HH:mm:ss", FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss", CHINA_TIMEZONE))
            .put("yyyy/MM/dd'T'HH:mm:ss.SSSZ", FastDateFormat.getInstance("yyyy/MM/dd'T'HH:mm:ss.SSSZ", CHINA_TIMEZONE))
            .put("yyyy/M/dd", FastDateFormat.getInstance("yyyy/M/dd", CHINA_TIMEZONE))
            .put("yyyy/MM", FastDateFormat.getInstance("yyyy/MM", CHINA_TIMEZONE))
            .put("yyyy/M", FastDateFormat.getInstance("yyyy/M", CHINA_TIMEZONE))
            .build();
    private static final Map<Pattern, FastDateFormat> defaultPatterns = ImmutableMap.<Pattern, FastDateFormat>builder()
            .put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"), DATE_FORMAT)
            .put(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), TIME_FORMAT)
            .put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}'T'\\d{2}:\\d{2}:\\d{2}.\\d{3}.*"), STANDARD_FORMAT)
            .put(Pattern.compile("\\d{8}"), defaultFormats.get("yyyyMMdd"))
            .put(Pattern.compile("\\d{14}"), defaultFormats.get("yyyyMMddHHmmss"))
            .put(Pattern.compile("\\d{17}"), defaultFormats.get("yyyyMMddHHmmssSSS"))
            .put(Pattern.compile("\\d{4}-\\d{1,2}-\\d{2}"), defaultFormats.get("yyyy-M-dd"))
            .put(Pattern.compile("\\d{4}-\\d{2}"), defaultFormats.get("yyyy-MM"))
            .put(Pattern.compile("\\d{4}-\\d{1,2}"), defaultFormats.get("yyyy-M"))
            .put(Pattern.compile("\\d{13,14}"), defaultFormats.get("yyyyMddHHmmss"))
            .put(Pattern.compile("\\d{6}"), defaultFormats.get("yyyyMM"))
            .put(Pattern.compile("\\d{5,6}"), defaultFormats.get("yyyyM"))
            .put(Pattern.compile("\\d{4}/\\d{2}/\\d{2}"), defaultFormats.get("yyyy/MM/dd"))
            .put(Pattern.compile("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"), defaultFormats.get("yyyy/MM/dd HH:mm:ss"))
            .put(Pattern.compile("\\d{4}/\\d{2}/\\d{2}'T'\\d{2}:\\d{2}:\\d{2}.\\d{3}.*"), defaultFormats.get("yyyy/MM/dd'T'HH:mm:ss.SSSZ"))
            .put(Pattern.compile("\\d{4}/\\d{1,2}/\\d{2}"), defaultFormats.get("yyyy/M/dd"))
            .put(Pattern.compile("\\d{4}/\\d{2}"), defaultFormats.get("yyyy/MM"))
            .put(Pattern.compile("\\d{4}/\\d{1,2}"), defaultFormats.get("yyyy/M"))
            .build();
    private static final Long millsPerDay = 24 * 3600000l;
    public static Pattern TIME_PATTERN = Pattern.compile("(-?\\d+)(y|M|d|h|m|s|ms)");
    public static Pattern TIMES_PATTERN = Pattern.compile("((-?\\d+)(y|M|d|h|m|s|ms))+");

    public static Date parseDate(String dateStr, String... patterns) {
        Preconditions.checkArgument(StringUtils.isNotBlank(dateStr), "无法将空字符串转换为Date对象");
        Preconditions.checkArgument(patterns.length > 0, "parseDate时必须指定至少一种格式");

        Date result = Stream.of(patterns).map(pattern -> {
            FastDateFormat format;
            if (defaultFormats.containsKey(pattern)) {
                format = defaultFormats.get(pattern);
            } else {
                format = FastDateFormat.getInstance(pattern, CHINA_TIMEZONE);
            }

            try {
                return format.parse(dateStr);
            } catch (Exception e) {
                return null;
            }
        }).filter(date -> date != null).findFirst().orElse(null);

        if (result == null) {
            log.warn("无法将{}换格式转换为格式{}的时间对象", dateStr, Arrays.asList(patterns));
        }

        return result;
    }

    public static Date parseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }

        Pattern pattern = defaultPatterns.keySet().stream().filter(ptn ->
                ptn.matcher(dateStr).matches()
        ).findAny().orElse(null);

        if (pattern != null) {
            try {
                return defaultPatterns.get(pattern).parse(dateStr);
            } catch (Exception e) {
                log.error("使用pattern[{}]解析时间串异常", pattern.pattern(), dateStr);
                return null;
            }
        } else if (NumberUtils.isDigits(dateStr)) {
            if (dateStr.length() == 10) {
                return new Date(Long.parseLong(dateStr) * 1000);
            } else if (dateStr.length() == 13) {
                return new Date(Long.parseLong(dateStr));
            } else {
                log.error("无法将{}转换为时间", dateStr);
                return null;
            }
        } else {
            log.error("无法找到合适的pattern解析时间串[{}]", dateStr);
            return null;
        }
    }

    public static String format(Date date) {
        Preconditions.checkArgument(date != null);
        return STANDARD_FORMAT.format(date);
    }

    public static String format(Date date, String pattern) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(pattern), "pattern不能为空");
        return DateFormatUtils.format(date, pattern, CHINA_TIMEZONE);
    }

    public static Date add(Date date, int calendarField, int amount) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        if (amount == 0) {
            return date;
        }

        switch (calendarField) {
            case Calendar.YEAR:
                return DateUtils.addYears(date, amount);
            case Calendar.MONTH:
                return DateUtils.addMonths(date, amount);
            case Calendar.DATE:
                return DateUtils.addDays(date, amount);
            case Calendar.HOUR:
                return DateUtils.addHours(date, amount);
            case Calendar.MINUTE:
                return DateUtils.addMinutes(date, amount);
            case Calendar.SECOND:
                return DateUtils.addSeconds(date, amount);
            case Calendar.MILLISECOND:
                return DateUtils.addMilliseconds(date, amount);
            default:
                throw new IllegalArgumentException("不支持的calendar类型");
        }
    }

    public static Date add(Date date, String... timeSpans) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        Preconditions.checkArgument(timeSpans != null, "时间间隔不能为空");

        for (String timeSpan : timeSpans) {
            Preconditions.checkArgument(StringUtils.isNotBlank(timeSpan), "时间间隔不能为空");
            timeSpan = timeSpan.replaceAll("[\\s\\u00A0]{1,}", "");
            if (!TIMES_PATTERN.matcher(timeSpan).matches()) {
                throw new IllegalArgumentException("timeSpan[" + timeSpan + "]不符合格式:" + TIME_PATTERN.toString());
            }

            Matcher matcher = TIME_PATTERN.matcher(timeSpan);
            while (matcher.find()) {
                switch (matcher.group(2)) {
                    case "y":
                        date = BaseDateUtils.add(date, Calendar.YEAR, Integer.valueOf(matcher.group(1)));
                        break;
                    case "M":
                        date = BaseDateUtils.add(date, Calendar.MONTH, Integer.valueOf(matcher.group(1)));
                        break;
                    case "d":
                        date = BaseDateUtils.add(date, Calendar.DAY_OF_MONTH, Integer.valueOf(matcher.group(1)));
                        break;
                    case "h":
                        date = BaseDateUtils.add(date, Calendar.HOUR, Integer.valueOf(matcher.group(1)));
                        break;
                    case "m":
                        date = BaseDateUtils.add(date, Calendar.MINUTE, Integer.valueOf(matcher.group(1)));
                        break;
                    case "s":
                        date = BaseDateUtils.add(date, Calendar.SECOND, Integer.valueOf(matcher.group(1)));
                        break;
                    case "ms":
                        date = BaseDateUtils.add(date, Calendar.MILLISECOND, Integer.valueOf(matcher.group(1)));
                        break;
                    default:
                        throw new IllegalArgumentException("timeSpan[" + timeSpan + "]不符合格式:" + TIME_PATTERN.toString());
                }
            }
        }
        return date;
    }

    public static String toDateFormat(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        return DATE_FORMAT.format(date);
    }

    public static String toDateFormat(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        return toDateFormat(parseDate(s));
    }

    public static Date fromDateFormat(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("格式不符合yyyy-MM-dd");
        }
    }

    public static String toTimeFormat(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        return TIME_FORMAT.format(date);
    }


    public static Date fromTimeFormat(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }

        try {
            return TIME_FORMAT.parse(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("格式不符合yyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 按中国时区. e.g. 如果当前为1月1日0点, 此时在美国将会是去年12月31号, 这时返回的时间将会是1月1日,
     */
    public static Date getMonthStart() {
        return getMonthStart(new Date());
    }

    /**
     * 按中国时区.
     */
    public static Date getMonthStart(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        String chinaDate = toDateFormat(date);
        chinaDate = chinaDate.substring(0, 8) + "01";
        return fromDateFormat(chinaDate);
    }


    /**
     * 按中国时区.
     */
    public static Date getMonthEnd() {
        return getMonthEnd(new Date());
    }

    /**
     * 按中国时区.
     */
    public static Date getMonthEnd(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        Date nextMonthStart = getNextMonthStart(date);
        return DateUtils.addSeconds(nextMonthStart, -1);
    }

    /**
     * 按中国时区.
     */
    public static Date getNextMonthStart() {
        return getNextMonthStart(new Date());
    }

    public static Date getNextMonthStart(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        String chinaDate = toDateFormat(date);
        int month = Integer.parseInt(chinaDate.substring(5, 7)) + 1;
        String sMonth = month + "";
        if (month < 10) {
            sMonth = "0" + sMonth;
        }
        return fromDateFormat(chinaDate.substring(0, 5) + sMonth + "-01");
    }

    public static Date getDayStart() {
        return getDayStart(new Date());
    }

    public static Date getDayStart(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        return parseDate(format(date, "yyyy-MM-dd"), "yyyy-MM-dd");
    }

    public static Date getDayEnd(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        return parseDate(format(date, "yyyy-MM-dd") + " 23:59:59", "yyyy-MM-dd HH:mm:ss");
    }


    public static boolean isSameYear(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        Calendar calendar1 = DateUtils.toCalendar(date1);
        Calendar calendar2 = DateUtils.toCalendar(date2);
        return (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR));
    }

    public static boolean isSameMonth(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        Calendar calendar1 = DateUtils.toCalendar(date1);
        Calendar calendar2 = DateUtils.toCalendar(date2);
        return (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) &&
                (calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH));
    }

    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        Calendar calendar1 = DateUtils.toCalendar(date1);
        Calendar calendar2 = DateUtils.toCalendar(date2);
        return calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }

    public static int getDayOfMonth(Date date) {
        Preconditions.checkArgument(date != null, "时间对象不能为空");
        return DateUtils.toCalendar(date).get(Calendar.DAY_OF_MONTH);
    }

    public static int compareDay(Date date1, Date date2) {
        Preconditions.checkArgument(date1 != null && date2 != null, "时间对象不能为空");
        date1 = getDayStart(date1);
        date2 = getDayStart(date2);
        return (int) ((date1.getTime() - date2.getTime()) / millsPerDay);
    }

    public static int compareMonth(Date date1, Date date2) {
        Preconditions.checkArgument(date1 != null && date2 != null, "时间对象不能为空");
        Calendar calendar1 = DateUtils.toCalendar(date1);
        Calendar calendar2 = DateUtils.toCalendar(date2);
        int year1 = calendar1.get(Calendar.YEAR);
        int year2 = calendar2.get(Calendar.YEAR);
        int month1 = calendar1.get(Calendar.MONTH);
        int month2 = calendar2.get(Calendar.MONTH);
        return (month1 - month2) + (year1 - year2) * 12;
    }

    public static int compareYear(Date date1, Date date2) {
        Preconditions.checkArgument(date1 != null && date2 != null, "时间对象不能为空");
        int year1 = DateUtils.toCalendar(date1).get(Calendar.YEAR);
        int year2 = DateUtils.toCalendar(date2).get(Calendar.YEAR);
        return year1 - year2;
    }
}
