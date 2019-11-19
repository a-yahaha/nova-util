package com.xss.common.nova.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseMathUtils {
    private static Pattern CALC_PTN = Pattern.compile("([-+/*]?)[^-+/*]+");
    public static String multiplyTime(String src, Integer number) {
        if(StringUtils.isBlank(src)) {
            return null;
        }

        src = src.replaceAll("[\\s\\u00A0]{1,}", "");
        if (!BaseDateUtils.TIMES_PATTERN.matcher(src).matches()) {
            throw new IllegalArgumentException("无效的时间字符串:" + src);
        }

        StringBuilder buf = new StringBuilder();
        Matcher matcher = BaseDateUtils.TIME_PATTERN.matcher(src);
        while (matcher.find()) {
            int value = Integer.valueOf(matcher.group(1));
            buf.append(value * number).append(matcher.group(2));
        }

        if (buf.length() == 0) {
            throw new IllegalArgumentException("无效的时间字符串:" + src);
        }

        return buf.toString();
    }

    public static Long yuan2Fen(BigDecimal yuan) {
        return yuan.multiply(new BigDecimal(100)).longValue();
    }

    public static BigDecimal fen2Yuan(Long fen) {
        return new BigDecimal(fen).divide(new BigDecimal(100));
    }

    /**
     * 将计算表达式(如a+b-c)解析并按从左到右的顺序依次执行(乘法并不会优先于加减法进行运算)
     * a,如果feeMap中存在key a的话a的值取pair中第一个值, pair中第二个值用于给prefix+a赋值
     */
    public static BigDecimal calculate(Map<String, Pair<BigDecimal, BigDecimal>> feeMap, String src, String prefix) {
        if(StringUtils.isBlank(src)) {
            return null;
        }
        src = src.replaceAll("[\\s\\u00A0]{1,}", "");

        BigDecimal result = new BigDecimal(0);
        Matcher matcher = CALC_PTN.matcher(src);
        while(matcher.find()) {
            String operator = matcher.group(1);
            String item = matcher.group();
            if(StringUtils.isNotBlank(operator)) {
                operator = matcher.group(1);
                item = item.substring(1);
            } else {
                operator = "+";
            }

            BigDecimal value = null;
            if(feeMap.containsKey(item)) {
                value = feeMap.get(item).getLeft();
            } else if(StringUtils.isNotBlank(prefix) && item.startsWith(prefix)) {
                item = item.substring(prefix.length());
                if(feeMap.containsKey(item)) {
                    value = feeMap.get(item).getRight();
                }
            }

            if(value == null) {
                throw new IllegalArgumentException("无法在"+feeMap.toString()+"中找到'"+item+"'对应的值");
            }

            switch (operator) {
                case "-": result = result.subtract(value); break;
                case "*": result = result.multiply(value); break;
                case "/": result = result.divide(value); break;
                default: result = result.add(value);
            }
        }

        return result;
    }
}
