package com.xss.common.nova.util;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;

@Slf4j
public class BaseNumberUtil {
    public static Long yuanToFen(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(new BigDecimal(100)).longValue();
    }

    public static Long yuanToFen(String amount) {
        if(StringUtils.isBlank(amount) || !NumberUtils.isNumber(amount)) {
            return null;
        }

        return new BigDecimal(amount).multiply(new BigDecimal(100)).longValue();
    }

    public static Long yuanToFenNotNull(BigDecimal amount) {
        if (amount == null) {
            return 0l;
        }
        return amount.multiply(new BigDecimal(100)).longValue();
    }

    public static int compare(BigDecimal num1, BigDecimal num2) {
        if (num1 == null && num2 == null) {
            return 0;
        }
        if (num1 == null) {
            return -1;
        }
        if (num2 == null) {
            return 1;
        }
        return num1.compareTo(num2);
    }

    public static String numToString(Integer num, Integer len, char fillChar) {
        Preconditions.checkArgument(num != null, "num不能为null");
        String result = num + "";
        if (result.length() > len) {
            return result.substring(0, len);
        } else {
            return StringUtils.repeat(fillChar, len - result.length()) + result;
        }
    }
}
