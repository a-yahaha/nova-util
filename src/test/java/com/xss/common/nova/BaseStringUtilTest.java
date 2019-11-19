package com.xss.common.nova;


import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.util.BaseDateUtil;
import com.xss.common.nova.util.BaseStringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.set.SynchronizedSet;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@Slf4j
public class BaseStringUtilTest {
    @JsonMosaic(start = 0, length = 4)
    private String text;

    @Test
    public void test() {
        String day = BaseDateUtil.toDateFormat(new Date());
        String uuid = BaseStringUtil.digitalUUID();
        assertTrue(uuid.length() == 20);
        assertTrue(NumberUtils.isNumber(uuid));
        Date date = BaseStringUtil.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        uuid = BaseStringUtil.digitalUUID(17);
        assertTrue(uuid.length() == 17);
        assertTrue(NumberUtils.isNumber(uuid));
        date = BaseStringUtil.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        uuid = BaseStringUtil.digitalUUID(35);
        assertTrue(uuid.length() == 35);
        assertTrue(NumberUtils.isNumber(uuid));
        date = BaseStringUtil.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        uuid = BaseStringUtil.digitalUUID(36);
        assertTrue(uuid.length() == 36);
        assertTrue(NumberUtils.isNumber(uuid));
        date = BaseStringUtil.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        long lid = BaseStringUtil.longUid();
        date = BaseStringUtil.timeFromLongUid(lid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        for (int i = 0; i < 100; i++) {
            long idOfSameGene = BaseStringUtil.longUid(lid, 10);
            assertEquals(lid % 1024, idOfSameGene % 1024);
        }

        for (int i = 0; i < 100; i++) {
            long idOfSameGene = BaseStringUtil.longUid(lid, 8);
            assertEquals(lid % 256, idOfSameGene % 256);
        }

        lid = BaseStringUtil.longUid("123456");
        date = BaseStringUtil.timeFromLongUid(lid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        lid = BaseStringUtil.longUid("123456", "wyb", "hello", "world");
        date = BaseStringUtil.timeFromLongUid(lid);
        assertEquals(day, BaseDateUtil.toDateFormat(date));

        uuid = BaseStringUtil.uuidSimple();
        assertEquals(BaseDateUtil.toDateFormat(new Date(BaseStringUtil.timeFromUUID(uuid))), day);

        //批量生成uid
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Set<String> set = SynchronizedSet.decorate(new HashSet<>());
        int threadSize = 100;
        int fetchSize = 10000;
        for (int i = 0; i < threadSize; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < fetchSize / threadSize; j++) {
                    String id = BaseStringUtil.timeUUID(4, null, null);
                    set.add(id);
                }
            });
        }
        executorService.shutdown();
        try {
            while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            assertEquals(fetchSize, set.size());
        }
    }

    @Test
    public void testCompare() {
        String s1 = "1234";
        String s2 = "1234";
        assertEquals(1, BaseStringUtil.compare(s1, s2, -1));
        s1 = "12*4";
        assertEquals(0, BaseStringUtil.compare(s1, s2, -1));
        assertEquals(1, BaseStringUtil.compare(s1, s2, 3));
        s2 = "1#34";
        assertEquals(0, BaseStringUtil.compare(s1, s2, -1));
        assertEquals(1, BaseStringUtil.compare(s1, s2, 2));
        s1 = "****";
        assertEquals(0, BaseStringUtil.compare(s1, s2, -1));
        s1 = "*******";
        assertEquals(0, BaseStringUtil.compare(s1, s2, -1));
        s1 = "*******###";
        assertEquals(0, BaseStringUtil.compare(s1, s2, -1));
        s1 = "*******###&&";
        assertEquals(-1, BaseStringUtil.compare(s1, s2, -1));
        s1 = "1#345";
        assertEquals(-1, BaseStringUtil.compare(s1, s2, -1));
    }

    @Test
    public void testMergeSave() {
        BaseStringUtil.mergeSave(" ", null);
        String s1 = "1234";
        String s2 = "1234";
        assertEquals("1234", BaseStringUtil.mergeSave(s1, s2));
        s1 = "12*4";
        assertEquals("1234", BaseStringUtil.mergeSave(s1, s2));
        s2 = "1#34";
        assertEquals("1234", BaseStringUtil.mergeSave(s1, s2));
        s1 = "****";
        assertEquals("1#34", BaseStringUtil.mergeSave(s1, s2));
        s1 = "*******";
        assertEquals("1#34", BaseStringUtil.mergeSave(s1, s2));
        s1 = "*******###";
        assertEquals("1#34", BaseStringUtil.mergeSave(s1, s2));
        s1 = "*******###&&";
        assertEquals("1#34", BaseStringUtil.mergeSave(s1, s2));
        s1 = null;
        assertEquals("1#34", BaseStringUtil.mergeSave(s1, s2));
        s1 = "4567";
        assertEquals("4567", BaseStringUtil.mergeSave(s1, s2));
    }

    @Test
    public void testMerge() {
        String s1 = "1234";
        String s2 = "1234";
        assertEquals("1234", BaseStringUtil.merge(s1, s2));
        s1 = "12*4";
        assertEquals("1234", BaseStringUtil.merge(s1, s2));
        s2 = "1#34";
        assertEquals("1234", BaseStringUtil.merge(s1, s2));
        s1 = "****";
        assertEquals("1#34", BaseStringUtil.merge(s1, s2));
        s1 = "*******";
        assertEquals("1#34", BaseStringUtil.merge(s1, s2));
        s1 = "*******###";
        assertEquals("1#34", BaseStringUtil.merge(s1, s2));
        s1 = "*******###&&";
        assertEquals(null, BaseStringUtil.merge(s1, s2));
        s1 = null;
        assertEquals("1#34", BaseStringUtil.merge(s1, s2));
        s1 = "4567";
        assertEquals(null, BaseStringUtil.merge(s1, s2));
    }

    @Test
    public void testContains() {
        assertTrue(BaseStringUtil.contains("123", "1", null));
        assertTrue(BaseStringUtil.contains("123", "2", null));
        assertTrue(BaseStringUtil.contains("123", "3", null));
        assertTrue(BaseStringUtil.contains("123", "1", "*"));
        assertTrue(BaseStringUtil.contains("123", "2", "*#"));
        assertTrue(BaseStringUtil.contains("123", "3", "#"));
        assertTrue(BaseStringUtil.contains("123", "1*", "*"));
        assertTrue(BaseStringUtil.contains("123", "2*", "*#"));
        assertTrue(BaseStringUtil.contains("123", "#2", "*#"));
        assertTrue(BaseStringUtil.contains("123", "#3", "#"));
        assertTrue(BaseStringUtil.contains("123", "*2*", "*"));
        assertFalse(BaseStringUtil.contains("123", "*2#", "*"));
        assertFalse(BaseStringUtil.contains("123", "32", "*"));
    }

    @Test
    public void testCollapse() {
        assertEquals("w y", BaseStringUtil.collapseWhiteSpace("w     y"));
        assertEquals("w y", BaseStringUtil.collapseWhiteSpace("w y"));
        assertEquals("wy", BaseStringUtil.collapseWhiteSpace("wy"));
        assertEquals("", BaseStringUtil.collapseWhiteSpace(""));
        assertEquals(null, BaseStringUtil.collapseWhiteSpace(null));
    }

    @Test
    public void testUnderScoreToCamel() {
        assertEquals("testHello", BaseStringUtil.underScoreToCamel("test_hello", false));
        assertEquals("TestHello", BaseStringUtil.underScoreToCamel("test_hello", true));
        assertEquals("testHello", BaseStringUtil.underScoreToCamel("testHello", false));
        assertEquals("TestHello", BaseStringUtil.underScoreToCamel("testHello", true));
        assertEquals("tESTHELLO", BaseStringUtil.underScoreToCamel("TEST_HELLO", false));
        assertEquals("TESTHELLO", BaseStringUtil.underScoreToCamel("TEST_HELLO", true));
        assertEquals("TEsTHeLLO", BaseStringUtil.underScoreToCamel("TEsT_HeLLO", true));
        assertEquals("TestHello0", BaseStringUtil.underScoreToCamel("test_hello_0", true));
    }

    @Test
    public void testMosaic() throws Exception {
        String src = "123";
        assertEquals("*23", BaseStringUtil.mosaic(src, 0, 1, '*'));
        assertEquals("1*3", BaseStringUtil.mosaic(src, 1, 2, '*'));
        assertEquals("12*", BaseStringUtil.mosaic(src, 2, 3, '*'));
        assertEquals("123", BaseStringUtil.mosaic(src, 1, 1, '*'));
        assertEquals("1", BaseStringUtil.mosaic("1", 1, 1, '*'));
        assertEquals("1**4", BaseStringUtil.mosaic("1234", 1, 3, '*'));
        assertEquals("1***", BaseStringUtil.mosaic("1234", 1, 9, '*'));
        assertEquals("----56", BaseStringUtil.mosaic("123456",
                BaseStringUtilTest.class.getDeclaredField("text").getAnnotation(JsonMosaic.class), '-'));
    }

    @Test
    public void testJoin() {
        assertEquals("1", BaseStringUtil.joinIgnoreBlank(",", "1"));
        assertEquals("1,2,3", BaseStringUtil.joinIgnoreBlank(",", "1", "2", "3"));
        assertEquals("1,3", BaseStringUtil.joinIgnoreBlank(",", "1", null, "3"));
        assertEquals("1,3", BaseStringUtil.joinIgnoreBlank(",", "1", "", "3"));
        assertEquals("1,3", BaseStringUtil.joinIgnoreBlank(",", null, "1", "", "3"));
        assertEquals("", BaseStringUtil.joinIgnoreBlank(",", "", ""));
    }

    @Test
    public void testPluralAndSingular() {
        List<String> singulars = Arrays.asList("car", "address", "place", "piece", "child", "person",
                "sheep", "orange", "cat", "penny", "class", "party", "goods");

        List<String> plurals = Arrays.asList("cars", "addresses", "places", "pieces", "children", "people",
                "sheep", "oranges", "cats", "pence", "classes", "parties", "goods");

        IntStream.range(0, singulars.size()).forEach(index ->
                assertEquals(BaseStringUtil.singularize(plurals.get(index)), singulars.get(index)));
        IntStream.range(0, singulars.size()).forEach(index ->
                assertEquals(BaseStringUtil.pluralize(singulars.get(index)), plurals.get(index)));

        assertEquals(BaseStringUtil.pluralize("boys"), "boys");
        assertEquals(BaseStringUtil.singularize("boy"), "boy");
    }

    @Test
    public void testReplace() {
        String finalStr = BaseStringUtil.replacePlaceholders("${name}你好,你真的是${name}吗, 我是你的${relation}, 我今年${age}岁",
                ImmutableMap.of("name", "王师父", "relation", "粉丝", "age", 18));
        assertEquals("王师父你好,你真的是王师父吗, 我是你的粉丝, 我今年18岁", finalStr);
    }

    @Test
    public void testConvertCamel() {
        assertEquals(BaseStringUtil.convertCamel("userEntity", "-"), "user-entity");
        assertEquals(BaseStringUtil.convertCamel("userEntityHello", "-"), "user-entity-hello");
        assertEquals(BaseStringUtil.convertCamel("user", "-"), "user");
    }

    @Test
    public void testToCamel() {
        assertEquals(BaseStringUtil.toCamel("for_test", "_-"), "forTest");
        assertEquals(BaseStringUtil.toCamel("for__test", "_-"), "forTest");
    }

    @Test
    public void testCompress() {
        for (int i = 1; i < 500; i++) {
            String src = RandomStringUtils.randomAlphanumeric(i).toLowerCase();

            String compressed = BaseStringUtil.compress(src);
            String decompressed = BaseStringUtil.decompress(compressed);
            if (!src.equals(decompressed)) {
                log.info("src:{}, compress:{}, decompress:{}, result:{}", src, compressed, decompressed, src.equals(decompressed));
                assertTrue("压/解缩" + src + "结果不正确", false);
            }
        }
    }
}
