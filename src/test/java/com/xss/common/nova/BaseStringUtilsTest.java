package com.xss.common.nova;


import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.util.BaseDateUtils;
import com.xss.common.nova.util.BaseStringUtils;
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
public class BaseStringUtilsTest {
    @JsonMosaic(start = 0, length = 4)
    private String text;

    @Test
    public void test() {
        String day = BaseDateUtils.toDateFormat(new Date());
        String uuid = BaseStringUtils.digitalUUID();
        assertTrue(uuid.length() == 20);
        assertTrue(NumberUtils.isNumber(uuid));
        Date date = BaseStringUtils.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        uuid = BaseStringUtils.digitalUUID(17);
        assertTrue(uuid.length() == 17);
        assertTrue(NumberUtils.isNumber(uuid));
        date = BaseStringUtils.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        uuid = BaseStringUtils.digitalUUID(35);
        assertTrue(uuid.length() == 35);
        assertTrue(NumberUtils.isNumber(uuid));
        date = BaseStringUtils.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        uuid = BaseStringUtils.digitalUUID(36);
        assertTrue(uuid.length() == 36);
        assertTrue(NumberUtils.isNumber(uuid));
        date = BaseStringUtils.timeFromDigitalUUID(uuid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        long lid = BaseStringUtils.longUid();
        date = BaseStringUtils.timeFromLongUid(lid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        for (int i = 0; i < 100; i++) {
            long idOfSameGene = BaseStringUtils.longUid(lid, 10);
            assertEquals(lid % 1024, idOfSameGene % 1024);
        }

        for (int i = 0; i < 100; i++) {
            long idOfSameGene = BaseStringUtils.longUid(lid, 8);
            assertEquals(lid % 256, idOfSameGene % 256);
        }

        lid = BaseStringUtils.longUid("123456");
        date = BaseStringUtils.timeFromLongUid(lid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        lid = BaseStringUtils.longUid("123456", "wyb", "hello", "world");
        date = BaseStringUtils.timeFromLongUid(lid);
        assertEquals(day, BaseDateUtils.toDateFormat(date));

        uuid = BaseStringUtils.uuidSimple();
        assertEquals(BaseDateUtils.toDateFormat(new Date(BaseStringUtils.timeFromUUID(uuid))), day);

        //批量生成uid
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Set<String> set = SynchronizedSet.decorate(new HashSet<>());
        int threadSize = 100;
        int fetchSize = 10000;
        for (int i = 0; i < threadSize; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < fetchSize / threadSize; j++) {
                    String id = BaseStringUtils.timeUUID(4, null, null);
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
        assertEquals(1, BaseStringUtils.compare(s1, s2, -1));
        s1 = "12*4";
        assertEquals(0, BaseStringUtils.compare(s1, s2, -1));
        assertEquals(1, BaseStringUtils.compare(s1, s2, 3));
        s2 = "1#34";
        assertEquals(0, BaseStringUtils.compare(s1, s2, -1));
        assertEquals(1, BaseStringUtils.compare(s1, s2, 2));
        s1 = "****";
        assertEquals(0, BaseStringUtils.compare(s1, s2, -1));
        s1 = "*******";
        assertEquals(0, BaseStringUtils.compare(s1, s2, -1));
        s1 = "*******###";
        assertEquals(0, BaseStringUtils.compare(s1, s2, -1));
        s1 = "*******###&&";
        assertEquals(-1, BaseStringUtils.compare(s1, s2, -1));
        s1 = "1#345";
        assertEquals(-1, BaseStringUtils.compare(s1, s2, -1));
    }

    @Test
    public void testMergeSave() {
        BaseStringUtils.mergeSave(" ", null);
        String s1 = "1234";
        String s2 = "1234";
        assertEquals("1234", BaseStringUtils.mergeSave(s1, s2));
        s1 = "12*4";
        assertEquals("1234", BaseStringUtils.mergeSave(s1, s2));
        s2 = "1#34";
        assertEquals("1234", BaseStringUtils.mergeSave(s1, s2));
        s1 = "****";
        assertEquals("1#34", BaseStringUtils.mergeSave(s1, s2));
        s1 = "*******";
        assertEquals("1#34", BaseStringUtils.mergeSave(s1, s2));
        s1 = "*******###";
        assertEquals("1#34", BaseStringUtils.mergeSave(s1, s2));
        s1 = "*******###&&";
        assertEquals("1#34", BaseStringUtils.mergeSave(s1, s2));
        s1 = null;
        assertEquals("1#34", BaseStringUtils.mergeSave(s1, s2));
        s1 = "4567";
        assertEquals("4567", BaseStringUtils.mergeSave(s1, s2));
    }

    @Test
    public void testMerge() {
        String s1 = "1234";
        String s2 = "1234";
        assertEquals("1234", BaseStringUtils.merge(s1, s2));
        s1 = "12*4";
        assertEquals("1234", BaseStringUtils.merge(s1, s2));
        s2 = "1#34";
        assertEquals("1234", BaseStringUtils.merge(s1, s2));
        s1 = "****";
        assertEquals("1#34", BaseStringUtils.merge(s1, s2));
        s1 = "*******";
        assertEquals("1#34", BaseStringUtils.merge(s1, s2));
        s1 = "*******###";
        assertEquals("1#34", BaseStringUtils.merge(s1, s2));
        s1 = "*******###&&";
        assertEquals(null, BaseStringUtils.merge(s1, s2));
        s1 = null;
        assertEquals("1#34", BaseStringUtils.merge(s1, s2));
        s1 = "4567";
        assertEquals(null, BaseStringUtils.merge(s1, s2));
    }

    @Test
    public void testContains() {
        assertTrue(BaseStringUtils.contains("123", "1", null));
        assertTrue(BaseStringUtils.contains("123", "2", null));
        assertTrue(BaseStringUtils.contains("123", "3", null));
        assertTrue(BaseStringUtils.contains("123", "1", "*"));
        assertTrue(BaseStringUtils.contains("123", "2", "*#"));
        assertTrue(BaseStringUtils.contains("123", "3", "#"));
        assertTrue(BaseStringUtils.contains("123", "1*", "*"));
        assertTrue(BaseStringUtils.contains("123", "2*", "*#"));
        assertTrue(BaseStringUtils.contains("123", "#2", "*#"));
        assertTrue(BaseStringUtils.contains("123", "#3", "#"));
        assertTrue(BaseStringUtils.contains("123", "*2*", "*"));
        assertFalse(BaseStringUtils.contains("123", "*2#", "*"));
        assertFalse(BaseStringUtils.contains("123", "32", "*"));
    }

    @Test
    public void testCollapse() {
        assertEquals("w y", BaseStringUtils.collapseWhiteSpace("w     y"));
        assertEquals("w y", BaseStringUtils.collapseWhiteSpace("w y"));
        assertEquals("wy", BaseStringUtils.collapseWhiteSpace("wy"));
        assertEquals("", BaseStringUtils.collapseWhiteSpace(""));
        assertEquals(null, BaseStringUtils.collapseWhiteSpace(null));
    }

    @Test
    public void testUnderScoreToCamel() {
        assertEquals("testHello", BaseStringUtils.underScoreToCamel("test_hello", false));
        assertEquals("TestHello", BaseStringUtils.underScoreToCamel("test_hello", true));
        assertEquals("testHello", BaseStringUtils.underScoreToCamel("testHello", false));
        assertEquals("TestHello", BaseStringUtils.underScoreToCamel("testHello", true));
        assertEquals("tESTHELLO", BaseStringUtils.underScoreToCamel("TEST_HELLO", false));
        assertEquals("TESTHELLO", BaseStringUtils.underScoreToCamel("TEST_HELLO", true));
        assertEquals("TEsTHeLLO", BaseStringUtils.underScoreToCamel("TEsT_HeLLO", true));
        assertEquals("TestHello0", BaseStringUtils.underScoreToCamel("test_hello_0", true));
    }

    @Test
    public void testMosaic() throws Exception {
        String src = "123";
        assertEquals("*23", BaseStringUtils.mosaic(src, 0, 1, '*'));
        assertEquals("1*3", BaseStringUtils.mosaic(src, 1, 2, '*'));
        assertEquals("12*", BaseStringUtils.mosaic(src, 2, 3, '*'));
        assertEquals("123", BaseStringUtils.mosaic(src, 1, 1, '*'));
        assertEquals("1", BaseStringUtils.mosaic("1", 1, 1, '*'));
        assertEquals("1**4", BaseStringUtils.mosaic("1234", 1, 3, '*'));
        assertEquals("1***", BaseStringUtils.mosaic("1234", 1, 9, '*'));
        assertEquals("----56", BaseStringUtils.mosaic("123456",
                BaseStringUtilsTest.class.getDeclaredField("text").getAnnotation(JsonMosaic.class), '-'));
    }

    @Test
    public void testJoin() {
        assertEquals("1", BaseStringUtils.joinIgnoreBlank(",", "1"));
        assertEquals("1,2,3", BaseStringUtils.joinIgnoreBlank(",", "1", "2", "3"));
        assertEquals("1,3", BaseStringUtils.joinIgnoreBlank(",", "1", null, "3"));
        assertEquals("1,3", BaseStringUtils.joinIgnoreBlank(",", "1", "", "3"));
        assertEquals("1,3", BaseStringUtils.joinIgnoreBlank(",", null, "1", "", "3"));
        assertEquals("", BaseStringUtils.joinIgnoreBlank(",", "", ""));
    }

    @Test
    public void testPluralAndSingular() {
        List<String> singulars = Arrays.asList("car", "address", "place", "piece", "child", "person",
                "sheep", "orange", "cat", "penny", "class", "party", "goods");

        List<String> plurals = Arrays.asList("cars", "addresses", "places", "pieces", "children", "people",
                "sheep", "oranges", "cats", "pence", "classes", "parties", "goods");

        IntStream.range(0, singulars.size()).forEach(index ->
                assertEquals(BaseStringUtils.singularize(plurals.get(index)), singulars.get(index)));
        IntStream.range(0, singulars.size()).forEach(index ->
                assertEquals(BaseStringUtils.pluralize(singulars.get(index)), plurals.get(index)));

        assertEquals(BaseStringUtils.pluralize("boys"), "boys");
        assertEquals(BaseStringUtils.singularize("boy"), "boy");
    }

    @Test
    public void testReplace() {
        String finalStr = BaseStringUtils.replacePlaceholders("${name}你好,你真的是${name}吗, 我是你的${relation}, 我今年${age}岁",
                ImmutableMap.of("name", "王师父", "relation", "粉丝", "age", 18));
        assertEquals("王师父你好,你真的是王师父吗, 我是你的粉丝, 我今年18岁", finalStr);
    }

    @Test
    public void testConvertCamel() {
        assertEquals(BaseStringUtils.convertCamel("userEntity", "-"), "user-entity");
        assertEquals(BaseStringUtils.convertCamel("userEntityHello", "-"), "user-entity-hello");
        assertEquals(BaseStringUtils.convertCamel("user", "-"), "user");
    }

    @Test
    public void testToCamel() {
        assertEquals(BaseStringUtils.toCamel("for_test", "_-"), "forTest");
        assertEquals(BaseStringUtils.toCamel("for__test", "_-"), "forTest");
    }

    @Test
    public void testCompress() {
        for (int i = 1; i < 500; i++) {
            String src = RandomStringUtils.randomAlphanumeric(i).toLowerCase();

            String compressed = BaseStringUtils.compress(src);
            String decompressed = BaseStringUtils.decompress(compressed);
            if (!src.equals(decompressed)) {
                log.info("src:{}, compress:{}, decompress:{}, result:{}", src, compressed, decompressed, src.equals(decompressed));
                assertTrue("压/解缩" + src + "结果不正确", false);
            }
        }
    }
}
