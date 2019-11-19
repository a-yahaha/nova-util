package com.xss.common.nova.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.util.TimeUUIDUtils;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.model.Inflector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BaseStringUtils {
    private static final String IgnoreChars = "*#";
    private static final Integer MACHINE_MIN_LEN = 3;
    private static final Integer MACHINE_MAX_LEN = 6;
    private static final Date START_DATE = BaseDateUtils.fromDateFormat("2010-01-01");
    private static final Map<UidType, AtomicLong> randomMap = ImmutableMap.of(UidType.DIGITAL, new AtomicLong(0),
            UidType.TIME, new AtomicLong(0), UidType.LONG, new AtomicLong(0));
    private static List<String> SINGULARS = Arrays.asList("goods", "Goods", "penny", "Penny", "sms", "Sms", "data", "Data");
    private static List<String> PLURALS = Arrays.asList("goods", "Goods", "pence", "Pence", "smses", "Smses", "data", "Data");
    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");
    private static Pattern UPPER_CASE = Pattern.compile("[A-Z]");

    private static Pattern COMPRESS_SRC = Pattern.compile("([a-z]|[0-9]|[._])+");
    private static BiMap<Byte, Byte> dict = HashBiMap.create(64);

    static {
        dict.put((byte) 'a', (byte) 0);
        dict.put((byte) 'b', (byte) 1);
        dict.put((byte) 'c', (byte) 2);
        dict.put((byte) 'd', (byte) 3);
        dict.put((byte) 'e', (byte) 4);
        dict.put((byte) 'f', (byte) 5);
        dict.put((byte) 'g', (byte) 6);
        dict.put((byte) 'h', (byte) 7);
        dict.put((byte) 'i', (byte) 8);
        dict.put((byte) 'j', (byte) 9);
        dict.put((byte) 'k', (byte) 10);
        dict.put((byte) 'l', (byte) 11);
        dict.put((byte) 'm', (byte) 12);
        dict.put((byte) 'n', (byte) 13);
        dict.put((byte) 'o', (byte) 14);
        dict.put((byte) 'p', (byte) 15);
        dict.put((byte) 'q', (byte) 16);
        dict.put((byte) 'r', (byte) 17);
        dict.put((byte) 's', (byte) 18);
        dict.put((byte) 't', (byte) 19);
        dict.put((byte) 'u', (byte) 20);
        dict.put((byte) 'v', (byte) 21);
        dict.put((byte) 'w', (byte) 22);
        dict.put((byte) 'x', (byte) 23);
        dict.put((byte) 'y', (byte) 48);
        dict.put((byte) 'z', (byte) 49);
        dict.put((byte) '0', (byte) 50);
        dict.put((byte) '1', (byte) 51);
        dict.put((byte) '2', (byte) 52);
        dict.put((byte) '3', (byte) 53);
        dict.put((byte) '4', (byte) 54);
        dict.put((byte) '5', (byte) 55);
        dict.put((byte) '6', (byte) 56);
        dict.put((byte) '7', (byte) 57);
        dict.put((byte) '8', (byte) 58);
        dict.put((byte) '9', (byte) 59);
        dict.put((byte) '_', (byte) 60);
        dict.put((byte) '.', (byte) 61);
        dict.put((byte) 'A', (byte) 26);
        dict.put((byte) 'B', (byte) 27);
        dict.put((byte) 'C', (byte) 28);
        dict.put((byte) 'D', (byte) 29);
        dict.put((byte) 'E', (byte) 30);
        dict.put((byte) 'F', (byte) 31);
        dict.put((byte) 'G', (byte) 32);
        dict.put((byte) 'H', (byte) 33);
        dict.put((byte) 'I', (byte) 34);
        dict.put((byte) 'J', (byte) 35);
        dict.put((byte) 'K', (byte) 36);
        dict.put((byte) 'L', (byte) 37);
        dict.put((byte) 'M', (byte) 38);
        dict.put((byte) 'N', (byte) 39);
        dict.put((byte) 'O', (byte) 40);
        dict.put((byte) 'P', (byte) 41);
        dict.put((byte) 'Q', (byte) 42);
        dict.put((byte) 'R', (byte) 43);
        dict.put((byte) 'S', (byte) 44);
        dict.put((byte) 'T', (byte) 45);
        dict.put((byte) 'U', (byte) 46);
        dict.put((byte) 'V', (byte) 47);
        dict.put((byte) 'W', (byte) 24);
        dict.put((byte) 'X', (byte) 25);
        dict.put((byte) 'Y', (byte) 62);
        dict.put((byte) 'Z', (byte) 63);
    }

    /**
     * @param ignoredChars 比较s1和s2时可忽略的字符合集
     * @param matchCount   除开ignoredChars至少有多少个字符s1和s2相同
     * @return 1-s1和s2除开ignoredChars外其它字符都匹配且匹配的字符数大于或等于matchCount;
     * 0-s1和s2除开ignoredChars外其它字符都匹配但匹配的字符数小于matchCount;
     * -1-s1和s2有不匹配的字符且该字符不包含在IgnoredChars中
     */
    public static int compare(String s1, String s2, String ignoredChars, int matchCount) {
        if (ignoredChars == null) {
            return s1.equals(s2) ? 1 : -1;
        }

        if (length(s1, ignoredChars) == 0 || length(s2, ignoredChars) == 0) {
            return 0;
        }

        if (s1.length() != s2.length()) {
            return -1;
        }

        int count = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!StringUtils.contains(ignoredChars, s1.charAt(i)) && !StringUtils.contains(ignoredChars, s2.charAt(i))) {
                if (s1.charAt(i) == s2.charAt(i)) {
                    count++;
                } else {
                    return -1;
                }
            }
        }

        if (matchCount == -1) {
            matchCount = s1.length();
        }

        if (count >= matchCount) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int compare(String s1, String s2) {
        return compare(s1, s2, -1);
    }

    public static int compare(String s1, String s2, int matchCount) {
        return compare(s1, s2, IgnoreChars, matchCount);
    }

    /**
     * @param ignoredChars 比较s1和s2时可忽略的字符合集
     * @return null-如果s1,s2除开ignoredChars外有不相同的字符
     * 合并后的字符串- 12*4 与 1*34 在ignoredChars为 * 时合并的结果为 1234
     */
    public static String merge(String s1, String s2, String ignoredChars) {
        Preconditions.checkArgument(StringUtils.isNotBlank(ignoredChars), "ignoredChars不能为空");
        if (length(s1, ignoredChars) == 0) {
            return s2;
        }
        if (length(s2, ignoredChars) == 0) {
            return s1;
        }
        if (s1.length() != s2.length()) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            if (!StringUtils.contains(ignoredChars, s1.charAt(i)) && !StringUtils.contains(ignoredChars, s2.charAt(i))) {
                if (s1.charAt(i) == s2.charAt(i)) {
                    buf.append(s1.charAt(i));
                } else {
                    return null;
                }
            } else {
                buf.append(StringUtils.contains(ignoredChars, s1.charAt(i)) ? s2.charAt(i) : s1.charAt(i));
            }
        }
        return buf.toString();
    }

    public static String merge(String s1, String s2) {
        return merge(s1, s2, IgnoreChars);
    }

    public static String mergeSave(String s1, String s2) {
        return mergeSave(s1, s2, IgnoreChars);
    }

    /**
     * 返回合并后的字符串, 如无法合并返回有效字符数最多的字符串
     */
    public static String mergeSave(String s1, String s2, String ignoredChars) {
        String result = s2;
        if (length(s1, ignoredChars) != 0) {
            if (length(s2, ignoredChars) != 0) {
                String merged = BaseStringUtils.merge(s1, s2, "*#");
                if (StringUtils.isNotBlank(merged)) { //合并成功
                    result = merged;
                } else if (length(s1, ignoredChars) > length(s2, ignoredChars)) {
                    result = s1;
                }
            } else {
                result = s1;
            }
        } else if (s1 != null && s2 == null) {
            result = s1;
        }

        return result;
    }

    public static int length(String src, String ignoreChars) {
        if (StringUtils.isBlank(src)) {
            return 0;
        }

        return src.length() - ignoreChars.chars().map(c -> StringUtils.countMatches(src, (char) c)).sum();
    }

    public static String uuid() {
        return TimeUUIDUtils.getUniqueTimeUUIDinMillis().toString();
    }

    /**
     * 13位时间戳 + 3位机器名hash值 + 4位随机数 = 20位
     */
    public static String digitalUUID() {
        return digitalUUID(20, null);
    }

    public static String digitalUUID(Integer len) {
        return digitalUUID(len, null);
    }

    public static String digitalUUID(Integer len, Long suffix) {
        Preconditions.checkArgument(len > 16, "len必须在17位或以上");
        StringBuilder buf = new StringBuilder(System.currentTimeMillis() + "");

        //计算机器名及随机数的比例
        // 1. 最初分配最小位数给机器, 剩下的给随机数
        // 2. 如果随机数超过4, 分一半给机器
        // 3. 如果机器位数超过最大, 取最大
        int machineLen = MACHINE_MIN_LEN;
        int randomLen = len - 13 - machineLen;
        if (randomLen > 4) {
            int leftCount = randomLen - 4;

            //确定机器占用位数
            machineLen = 4 + leftCount / 2;
            if (machineLen > MACHINE_MAX_LEN) {
                machineLen = MACHINE_MAX_LEN;
            }

            randomLen = len - 13 - machineLen;
        }

        //添加机器码
        buf.append(machineId(machineLen));

        if (randomLen >= 16) {
            for (int i = 0; i < randomLen / 16; i++) {
                buf.append(RandomUtils.nextLong(1000000000000000l, 10000000000000000l));
            }
            randomLen = randomLen % 16;
        }
        if (randomLen > 0) {
            buf.append(innerRandom(UidType.DIGITAL, randomLen));
        }

        if (suffix != null) {
            buf.append(suffix);
        }
        return buf.toString();
    }

    public static Date timeFromDigitalUUID(String uuid) {
        Preconditions.checkArgument(StringUtils.isNotBlank(uuid), "uuid不能为空");
        Preconditions.checkArgument(uuid.length() >= 13, "uuid长度不能小于13");
        String timeStr = uuid.substring(0, 13);
        if (!NumberUtils.isDigits(timeStr)) {
            throw new RuntimeException("uuid[" + uuid + "]格式不正确");
        }
        return new Date(Long.parseLong(timeStr));
    }

    /**
     * 15位时间+3位机器码  +randomLen+prefix+suffix
     */
    public static String timeUUID(Integer randomLen, String prefix, String suffix) {
        Preconditions.checkArgument(randomLen != null && randomLen >= 0, "randomLen必须存在且不为负数");
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotBlank(prefix)) {
            buf.append(prefix);
        }
        buf.append(BaseDateUtils.format(new Date(), "yyMMddHHmmssSSS")).append(machineId(3)).append(innerRandom(UidType.TIME, randomLen));
        if (StringUtils.isNotBlank(suffix)) {
            buf.append(suffix);
        }
        return buf.toString();
    }

    /**
     * 12位时间戳(相对2010年1月1日) + 3位机器哈希 + 4位随机数
     */
    public static long longUid() {
        StringBuilder buf = new StringBuilder((System.currentTimeMillis() - START_DATE.getTime()) + "");
        if (buf.length() > 12 || buf.toString().compareTo("922337203684") > 0) {
            throw new RuntimeException("起始时间太小, 要更新了!");
        }

        buf.append(machineId(MACHINE_MIN_LEN));
        int randomLen = 7 - MACHINE_MIN_LEN;
        buf.append(innerRandom(UidType.LONG, randomLen));
        return Long.parseLong(buf.toString());
    }

    /**
     * @return 同longUid一样的规则生成uid, 但返回值下标[0,len-1]的bit值和gene一样
     * 该方法主要生成和gene经过 %pow(2,len)后有相同值的long型uid
     */
    public static long longUid(long gene, int len, String... args) {
        long uid = longUid(args);
        uid = uid >> len << len;
        BitSet set = BitSet.valueOf(new long[]{gene});
        set.clear(len, 64);
        set.or(BitSet.valueOf(new long[]{uid}));
        return set.toLongArray()[0];
    }

    public static long longUid(String... args) {
        StringBuilder buf = new StringBuilder((System.currentTimeMillis() - START_DATE.getTime()) + "");
        if (buf.length() > 12 || buf.toString().compareTo("922337203684") > 0) {
            throw new RuntimeException("起始时间太小, 要更新了!");
        }

        String argsHash = BaseSecurityUtils.md5(true, args);
        if (StringUtils.isBlank(argsHash)) {
            buf.append(innerRandom(UidType.LONG, 7));
        } else {
            buf.append(argsHash.substring(0, 7));
        }

        return Long.parseLong(buf.toString());
    }

    public static Date timeFromLongUid(long uid) {
        Long diff = Long.parseLong((uid + "").substring(0, 12));
        return new Date(START_DATE.getTime() + diff);
    }

    /**
     * 将正常uuid中的'-'去掉
     */
    public static String uuidSimple() {
        return uuid().replaceAll("-", "");
    }

    public static long timeFromUUID(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("uuid为空");
        }

        if (!uuid.contains("-")) {
            StringBuilder buf = new StringBuilder();
            buf.append(uuid.substring(0, 8)).append("-")
                    .append(uuid.substring(8, 12)).append("-")
                    .append(uuid.substring(12, 16)).append("-")
                    .append(uuid.substring(16, 20)).append("-")
                    .append(uuid.substring(20));
            uuid = buf.toString();
        }

        return TimeUUIDUtils.getTimeFromUUID(UUID.fromString(uuid));
    }

    public static boolean contains(String src, String contained) {
        return contains(src, contained, IgnoreChars);
    }

    public static boolean contains(String src, String contained, String ignoreChars) {
        if (StringUtils.isBlank(ignoreChars)) {
            return src.contains(contained);
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < contained.length(); i++) {
            if (StringUtils.contains(ignoreChars, contained.charAt(i))) {
                buf.append(".");
            } else {
                buf.append(contained.charAt(i));
            }
        }

        Matcher matcher = Pattern.compile(buf.toString()).matcher(src);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    public static String collapseWhiteSpace(String src) {
        if (src == null) {
            return null;
        }

        return src.replaceAll("[\\s\\u00A0]{2,}", " ");
    }

    public static String substringAfter(String src, String separator) {
        String result = StringUtils.substringAfter(src, separator);
        return StringUtils.defaultIfBlank(result, src);
    }

    /**
     * @return 下划线下的字符变成大写. 样例请参照BaseStringUtilsTest.java
     */
    public static String underScoreToCamel(String src, boolean firstLetterUpperCase) {
        return toCamel(src, "_", firstLetterUpperCase);
    }

    public static String toCamel(String src, String separators) {
        return toCamel(src, separators, null);
    }

    public static String toCamel(String src, String separators, Boolean firstLetterUpperCase) {
        if (StringUtils.isBlank(src)) {
            return src;
        }

        for (char c : separators.toCharArray()) {
            src = toCamel(src, c);
        }

        if (firstLetterUpperCase != null) {
            if (firstLetterUpperCase) {
                src = BaseStringUtils.convertFirstLetter(src, true);
            } else {
                src = BaseStringUtils.convertFirstLetter(src, false);
            }
        }
        return src;
    }

    private static String toCamel(String src, char separator) {
        if (StringUtils.isBlank(src)) {
            return src;
        }

        int lastIndex = 0;
        StringBuilder buf = new StringBuilder();
        Matcher matcher = Pattern.compile(Pattern.quote(separator + "") + "+").matcher(src);
        while (matcher.find()) {
            buf.append(src.substring(lastIndex, matcher.start()));
            buf.append(src.substring(matcher.end(), matcher.end() + 1).toUpperCase());
            lastIndex = matcher.end() + 1;
        }
        buf.append(src.substring(lastIndex));

        return buf.toString();
    }

    public static String convertCamel(String src, String separator) {
        return convertCamel(src, separator, null);
    }

    public static String convertCamel(String src, String separator, Boolean firstLetterUpperCase) { //userEntity -> user-entity
        if (StringUtils.isBlank(src)) {
            return src;
        }

        StringBuilder buf = new StringBuilder(src.substring(0, 1));
        src = src.substring(1);

        int lastIndex = 0;
        Matcher matcher = UPPER_CASE.matcher(src);
        while (matcher.find()) {
            buf.append(src.substring(lastIndex, matcher.start()));
            buf.append(separator).append(matcher.group().toLowerCase());
            lastIndex = matcher.start() + 1;
        }
        buf.append(src.substring(lastIndex));

        String result = buf.toString();
        if (firstLetterUpperCase != null) {
            if (firstLetterUpperCase) {
                result = BaseStringUtils.convertFirstLetter(result, true);
            } else {
                result = BaseStringUtils.convertFirstLetter(result, false);
            }
        }

        return result;
    }

    public static String mosaic(String text, JsonMosaic mosaic, char mosaicChar) {
        if (StringUtils.isBlank(text) || mosaic.start() >= text.length()) {
            return text;
        }

        if (mosaic.end() != 0) {
            if (mosaic.end() < 0) {
                text = BaseStringUtils.mosaic(text, mosaic.start(), text.length() + mosaic.end(), mosaicChar);
            } else {
                text = BaseStringUtils.mosaic(text, mosaic.start(), mosaic.end(), mosaicChar);
            }
        } else if (mosaic.length() != 0) {
            text = BaseStringUtils.mosaic(text, mosaic.start(), mosaic.start() + mosaic.length(), mosaicChar);
        } else {
            text = BaseStringUtils.mosaic(text, mosaic.start(), mosaic.start() + text.length(), mosaicChar);
        }
        return text;
    }

    public static String mosaic(String src, int startInclusive, int endExclusive, char mosaicChar) {
        if (StringUtils.isBlank(src)) {
            return src;
        }
        if (endExclusive > src.length()) {
            endExclusive = src.length();
        }
        Preconditions.checkArgument(endExclusive >= startInclusive, "endExclusive必须大于startInclusive");
        Preconditions.checkArgument(startInclusive >= 0, "startInclusive不能小于0");
        StringBuilder buf = new StringBuilder();
        if (startInclusive > 0) {
            buf.append(src.substring(0, startInclusive));
        }
        for (int i = startInclusive; i < endExclusive; i++) {
            buf.append(mosaicChar);
        }
        buf.append(src.substring(endExclusive));

        return buf.toString();
    }

    public static String joinIgnoreBlank(String sep, String... args) {
        Preconditions.checkArgument(sep != null, "分隔符不能为null");
        StringBuilder buf = new StringBuilder();
        int index = 0;
        for (String arg : args) {
            if (StringUtils.isNotBlank(arg)) {
                if (index != 0) {
                    buf.append(sep);
                }
                buf.append(arg);
                index++;
            }
        }

        return buf.toString();
    }

    public static String singularize(String plural) {
        if (PLURALS.contains(plural)) {
            return SINGULARS.get(PLURALS.indexOf(plural));
        }
        return Inflector.getInstance().singularize(plural);
    }

    public static String pluralize(String singular) {
        if (SINGULARS.contains(singular)) {
            return PLURALS.get(SINGULARS.indexOf(singular));
        }
        return Inflector.getInstance().pluralize(singular);
    }

    public static String replacePlaceholders(String src, Map<String, Object> map) {
        Properties prop = new Properties();
        if (map != null) {
            map.forEach((key, value) -> prop.put(key, value == null ? null : value.toString()));
        }
        return placeholderHelper.replacePlaceholders(src, prop);
    }

    public static String convertFirstLetter(String src, boolean upperCase) {
        if (StringUtils.isBlank(src)) {
            return src;
        }

        return (upperCase ? src.substring(0, 1).toUpperCase() : src.substring(0, 1).toLowerCase()) +
                (src.length() > 1 ? src.substring(1) : "");
    }

    public static String compress(String src) {
        if(StringUtils.isBlank(src)) {
            throw new IllegalArgumentException("入参不能为空");
        }
        if(!COMPRESS_SRC.matcher(src).matches()) {
            throw new IllegalArgumentException("只支持对小写字母,数字以及._的压缩");
        }

        List<Boolean> bitList = compressWithDict(src);
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bitList.size() / 6; i++) {
            buf.append(boolListToChar(bitList.subList(i * 6, (i + 1) * 6)));
        }

        int remainder = bitList.size() % 6;
        if (remainder != 0) {
            int index = bitList.size() - remainder;
            for (int i = 0; i < 6 - remainder; i++) {
                bitList.add(true);
            }
            buf.append(boolListToChar(bitList.subList(index, bitList.size())));
        }

        return buf.toString();
    }

    public static String decompress(String src) {
        List<Boolean> srcBitList = new ArrayList<>();
        src.chars().forEach(c -> {
            List<Boolean> list = byteToBoolList(dict.get(Byte.valueOf((byte) c)));
            int size = list.size();
            for (int i = 0; i < 6 - size; i++) {
                list.add(0, false);
            }
            srcBitList.addAll(list);
        });

        StringBuilder buf = new StringBuilder();
        int index = 0;
        while (index < srcBitList.size() - 4) {
            List<Boolean> list = srcBitList.subList(index, index + 2);
            if (!list.get(0) || (list.get(0) && !list.get(1))) {
                list = srcBitList.subList(index, index + 5);
                index += 5;
            } else {
                if (srcBitList.size() > index + 5) {
                    list = srcBitList.subList(index, index + 6);
                    index += 6;
                } else {
                    index += 6;
                    continue;
                }
            }
            buf.append(boolListToChar(list));
        }
        return buf.toString();
    }

    private static String toCamel(String word) {
        if (StringUtils.isBlank(word)) {
            return word;
        }
        word = Character.toUpperCase(word.charAt(0)) + word.substring(1);
        StringBuilder buf = new StringBuilder();
        Matcher matcher = Pattern.compile("[A-Z]{2,}").matcher(word);
        int end = 0;
        while (matcher.find()) {
            int start = matcher.start();
            end = matcher.end();
            buf.append(word.substring(0, start + 1));
            buf.append(matcher.group().substring(1).toLowerCase());
        }
        buf.append(word.substring(end));
        String result = buf.toString();
        return result.substring(0, result.length() - 1) + Character.toLowerCase(result.charAt(result.length() - 1));
    }

    private static String innerRandom(UidType uidType, int len) {
        AtomicLong randomValue = randomMap.get(uidType);
        long randomLong = randomValue.getAndIncrement();
        if (randomLong > Long.MAX_VALUE / 2) {
            randomValue.set(0);
        }

        String random = String.valueOf(randomLong);
        if (random.length() < len) {
            return StringUtils.leftPad(random, len, "0");
        } else {
            return random.substring(0, len);
        }
    }

    private static String machineId(int machineLen) {
        String hostName = BaseMachineUtils.getHostName();
        if (StringUtils.isBlank(hostName)) {
            return StringUtils.repeat("0", machineLen);
        } else {
            return BaseSecurityUtils.md5(true, hostName).substring(0, machineLen);
        }
    }

    private static List<Boolean> compressWithDict(String src) {
        List<Boolean> list = new ArrayList<>();
        src.chars().forEach(c -> {
            List<Boolean> tmpList = new ArrayList<>();
            int value = dict.get(Byte.valueOf((byte) c));
            tmpList = byteToBoolList(value);
            if (value < 24) {
                for (int i = 0; i < 5 - tmpList.size(); i++) {
                    list.add(false);
                }
            } else {
                for (int i = 0; i < 6 - tmpList.size(); i++) {
                    list.add(false);
                }
            }
            list.addAll(tmpList);
        });

        return list;
    }

    private static List<Boolean> byteToBoolList(int b) {
        List<Boolean> list = new ArrayList<>();
        do {
            list.add(0, b % 2 == 1);
            b = b / 2;
        } while (b != 0);

        return list;
    }

    private static char boolListToChar(List<Boolean> list) {
        byte value = 0;
        for (int i = 1; i < list.size() + 1; i++) {
            if (list.get(i - 1)) {
                value += 1 << (list.size() - i);
            }
        }

        return (char) dict.inverse().get(value).byteValue();
    }

    enum UidType {
        DIGITAL,
        TIME,
        LONG
    }
}
