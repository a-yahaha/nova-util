package com.xss.common.nova;

import com.xss.common.nova.model.Address;
import com.xss.common.nova.model.ComplexUser;
import com.xss.common.nova.model.Gender;
import com.xss.common.nova.util.BaseObjectUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import static org.junit.Assert.*;

public class BaseObjectUtilsTest {
    @Test
    public void test() {
        ComplexUser user = new ComplexUser();
        user.setName("brady");
        user.setAge(18);
        user.setGender(Gender.MALE);
        Address address = new Address();
        address.setCountry("china");
        address.setProvince("hz");
        user.setAddr(address);
        user.setOtherAddr(Arrays.asList(address));
        user.setTest(Arrays.asList("test1"));
        user.setTags(Collections.emptyMap());
        Map<String, String> map = BaseObjectUtils.toMap("user", user);

        assertTrue(map.containsKey("user.name"));
        assertEquals(map.get("user.name"), "brady");
        assertTrue(map.containsKey("user.age"));
        assertEquals(map.get("user.age"), "18");
        assertTrue(map.containsKey("user.gender"));
        assertEquals(map.get("user.gender"), "MALE");
        assertTrue(map.containsKey("user.addr.country"));
        assertEquals(map.get("user.addr.country"), "china");
        assertTrue(map.containsKey("user.addr.province"));
        assertEquals(map.get("user.addr.province"), "hz");

        Class cls = BaseObjectUtils.forName("Integer");
        assertEquals(cls, Integer.class);
        cls = BaseObjectUtils.forName("int");
        assertEquals(cls, int.class);
        cls = BaseObjectUtils.forName("Integer", true);
        assertEquals(cls, Integer.class);
        cls = BaseObjectUtils.forName("int", true);
        assertEquals(cls, Integer.class);
        cls = BaseObjectUtils.forName("com.xss.common.nova.BaseObjectUtilsTest", true);
        assertEquals(cls, BaseObjectUtilsTest.class);
    }
}
