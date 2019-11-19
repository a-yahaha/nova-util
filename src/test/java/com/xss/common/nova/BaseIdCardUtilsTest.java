package com.xss.common.nova;

import com.xss.common.nova.model.IdCardInfo;
import com.xss.common.nova.util.BaseIdCardUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaseIdCardUtilsTest {
    @Test
    public void testIdCard() {
        assertTrue(BaseIdCardUtils.parseIdCard("", true) == null);
        assertTrue(BaseIdCardUtils.parseIdCard("411481199102283323X", true) == null);

        IdCardInfo idCardInfo = BaseIdCardUtils.parseIdCard("41148119910228332X", true);
        assertEquals(idCardInfo.getProvince(), "河南省");
        assertEquals(idCardInfo.getCity(), "商丘市");
        assertEquals(idCardInfo.getCounty(), "永城市");
        assertEquals(idCardInfo.getBirthDate(), "1991-02-28");
        assertEquals(idCardInfo.isMale(), false);

        idCardInfo = BaseIdCardUtils.parseIdCard("130404198103212114", true);
        assertEquals(idCardInfo.getProvince(), "河北省");
        assertEquals(idCardInfo.getCity(), "邯郸市");
        assertEquals(idCardInfo.getCounty(), "复兴区");
        assertEquals(idCardInfo.getBirthDate(), "1981-03-21");
        assertEquals(idCardInfo.isMale(), true);

        idCardInfo = BaseIdCardUtils.parseIdCard("820000850213*82", false);
        assertEquals(idCardInfo.getProvince(), "澳门特别行政区");
        assertEquals(idCardInfo.getBirthDate(), "1985-02-13");
        assertEquals(idCardInfo.isMale(), false);

        idCardInfo = BaseIdCardUtils.parseIdCard("362330198411260232");
        assertEquals(idCardInfo.getProvince(), "江西省");
        assertEquals(idCardInfo.getCity(), "上饶市");
        assertEquals(idCardInfo.getCounty(), "波阳县");
    }
}
