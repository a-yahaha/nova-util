package com.xss.common.nova;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.util.BaseJsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class BaseJsonUtilsTest {
    @Test
    public void test() {
        Map<String, Object> map = ImmutableMap.of("type", "service", "version", 1);
        String json = BaseJsonUtils.writeValue(map);
        map = BaseJsonUtils.readValue(json, Map.class);
        assertTrue(map.containsKey("type"));
        assertEquals(map.get("type"), "service");
        assertTrue(map.containsKey("version"));
        assertEquals(map.get("version"), 1);

        map = ImmutableMap.of("f1", "hello");
        Bean bean = BaseJsonUtils.readValue(map, Bean.class);
        assertEquals(bean.getF1(), "hello");

        assertEquals(BaseJsonUtils.valueFromJsonKey(json, "type"), "service");

        String s = "[{\"f1\": \"1\"}]";
        List<Bean> list = BaseJsonUtils.readValues(s, Bean.class);
        assertTrue(list.size() == 1);
        assertEquals(list.get(0).getF1(), "1");

        s = "[]";
        list = BaseJsonUtils.readValues(s, Bean.class);
        assertTrue(list.size() == 0);

        s = "[{\"bean\": {\"f1\": \"11\"}, \"f2\":2}]";
        List<Map> mapList = BaseJsonUtils.readValues(s, Map.class);
        assertTrue(mapList.get(0).containsKey("f2"));
        assertTrue(mapList.get(0).containsKey("bean"));
        bean = BaseJsonUtils.readValue((Map) mapList.get(0).get("bean"), Bean.class);
        assertEquals(bean.getF1(), "11");

        s = "[1, 2]";
        List<Integer> intList = BaseJsonUtils.readValues(s, Integer.class);
        assertTrue(intList.size() == 2);
        assertTrue(intList.contains(1));

        s = "wyb";
        assertEquals(BaseJsonUtils.readValue(s, String.class), s);

        BeanWithMosaic beanWithMosaic = new BeanWithMosaic();
        beanWithMosaic.setF1("value1");
        beanWithMosaic.setF2("value2");
        assertTrue(BaseJsonUtils.writeValue(beanWithMosaic, true).contains("v**ue2"));

        VagueBean<Bean> vagueBean = BaseJsonUtils.readValue("{\"t\":{\"f1\":\"value.t.f1\"},\"f1\":\"value1\"}",
                new TypeReference<VagueBean<Bean>>() {
                }, true);
        assertTrue(vagueBean.getT().getF1().equals("value.t.f1"));
        assertTrue(vagueBean.getF1().equals("value1"));
    }

    @Data
    public static class Bean {
        private String f1;
    }

    @Data
    public static class BeanWithMosaic {
        private String f1;
        @JsonMosaic(start = 1, length = 2)
        private String f2;
    }

    @Data
    public static class VagueBean<T> {
        T t;
        private String f1;
    }
}
