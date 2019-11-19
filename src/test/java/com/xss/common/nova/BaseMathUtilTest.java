package com.xss.common.nova;

import com.xss.common.nova.util.BaseMathUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BaseMathUtilTest {
    @Test
    public void test() {
        assertEquals(BaseMathUtil.multiplyTime("1d", 5), "5d");
        assertEquals(BaseMathUtil.multiplyTime("1d1m", 5), "5d5m");
        assertEquals(BaseMathUtil.fen2Yuan(100L), new BigDecimal(1));
        assertEquals((long)BaseMathUtil.yuan2Fen(new BigDecimal(1)), 100L);

        Map<String, Pair<BigDecimal, BigDecimal>> feeMap = new HashMap<String, Pair<BigDecimal, BigDecimal>>() {{
            put("capital", Pair.of(new BigDecimal(1000), new BigDecimal(100)));
            put("fee", Pair.of(new BigDecimal(10), new BigDecimal(0)));
            put("lateFee", Pair.of(new BigDecimal(100), new BigDecimal(5)));
        }};
        assertEquals(BaseMathUtil.calculate(feeMap, "capital", null), new BigDecimal(1000));
        assertEquals(BaseMathUtil.calculate(feeMap, "left_capital", "left_"), new BigDecimal(100));
        assertEquals(BaseMathUtil.calculate(feeMap, "capital-left_capital/fee", "left_"), new BigDecimal(90));
    }
}
