package com.xss.common.nova;

import com.xss.common.nova.model.IdCardInfo;
import com.xss.common.nova.util.BaseIdCardUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaseIdCardUtilTest {
    @Test
    public void testIdCard() {
        assertTrue(BaseIdCardUtil.parseIdCard("", true) == null);
        assertTrue(BaseIdCardUtil.parseIdCard("411481********3323X", true) == null);

        IdCardInfo idCardInfo = BaseIdCardUtil.parseIdCard("411481********8332X", true);
        assertEquals(idCardInfo.getProvince(), "河南省");
        assertEquals(idCardInfo.getCity(), "商丘市");
        assertEquals(idCardInfo.getCounty(), "永城市");
        assertEquals(idCardInfo.getBirthDate(), "****-**-**");
        assertEquals(idCardInfo.isMale(), false);

        idCardInfo = BaseIdCardUtil.parseIdCard("130404********2114", true);
        assertEquals(idCardInfo.getProvince(), "河北省");
        assertEquals(idCardInfo.getCity(), "邯郸市");
        assertEquals(idCardInfo.getCounty(), "复兴区");
        assertEquals(idCardInfo.getBirthDate(), "****-**-**");
        assertEquals(idCardInfo.isMale(), true);

        idCardInfo = BaseIdCardUtil.parseIdCard("820000*******82", false);
        assertEquals(idCardInfo.getProvince(), "澳门特别行政区");
        assertEquals(idCardInfo.getBirthDate(), "1985-02-13");
        assertEquals(idCardInfo.isMale(), false);

        idCardInfo = BaseIdCardUtil.parseIdCard("362330********0232");
        assertEquals(idCardInfo.getProvince(), "江西省");
        assertEquals(idCardInfo.getCity(), "上饶市");
        assertEquals(idCardInfo.getCounty(), "波阳县");
    }
}
