package com.xss.common.nova.util;

import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.model.DistrictInfo;
import com.xss.common.nova.model.IdCardInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class BaseIdCardUtils {
    private static Map<String, String> provinces = ImmutableMap.<String, String>builder()
            .put("11", "北京市").put("12", "天津市").put("13", "河北省")
            .put("14", "山西省").put("15", "内蒙古自治区").put("21", "辽宁省")
            .put("22", "吉林省").put("23", "黑龙江省").put("31", "上海市")
            .put("32", "江苏省").put("33", "浙江省").put("34", "安徽省")
            .put("35", "福建省").put("36", "江西省").put("37", "山东省")
            .put("41", "河南省").put("42", "湖北省").put("43", "湖南省")
            .put("44", "广东省").put("45", "广西壮族自治区").put("46", "海南省")
            .put("50", "重庆市").put("51", "四川省").put("52", "贵州省")
            .put("53", "云南省").put("54", "西藏自治区").put("61", "陕西省")
            .put("62", "甘肃省").put("63", "青海省").put("64", "宁夏回族自治区")
            .put("65", "新疆维吾尔自治区").put("71", "台湾省").put("81", "香港特别行政区")
            .put("82", "澳门特别行政区").put("91", "国外").build();
    private static int powers[] = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static Map<String, DistrictInfo> districtInfoMap = new HashMap<>(3300);

    static {
        Properties prop = BasePropertiesUtils.load("district-info.properties");
        prop.entrySet().forEach(entry ->
                districtInfoMap.put((String) entry.getKey(), BaseJsonUtils.readValue((String) entry.getValue(), DistrictInfo.class))
        );
    }

    public static IdCardInfo parseIdCard(String idCard) {
        return parseIdCard(idCard, true);
    }

    public static IdCardInfo parseIdCard(String idCard, Boolean check) {
        if (StringUtils.isBlank(idCard) || (idCard.length() != 15 && idCard.length() != 18)) {
            return null;
        }

        if (check && !Pattern.matches("(\\d{15})|(\\d{17}(\\d|x|X))", idCard)) {
            return null;
        }

        IdCardInfo info = new IdCardInfo();
        //获取归属地信息
        DistrictInfo districtInfo = districtInfo(idCard.substring(0, 6));
        info.setProvince(districtInfo.getProvince());
        info.setCity(districtInfo.getCity());
        info.setCounty(districtInfo.getCounty());

        //获取出生日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        int index = 14;
        if (idCard.length() == 15) {
            dateFormat = new SimpleDateFormat("yyMMdd");
            index = 12;
        }
        String birthDate = idCard.substring(6, index);
        if (StringUtils.isNumeric(birthDate)) {
            try {
                Date date = dateFormat.parse(birthDate);
                String tmpDate = dateFormat.format(date);
                if (!tmpDate.equals(birthDate)) {// 身份证日期错误
                    return null;
                }
                info.setBirthDate(BaseDateUtils.toDateFormat(date));
                info.setAge(BaseDateUtils.compareYear(new Date(), date));
            } catch (ParseException e1) {
                return null;
            }
        }

        //获取性别
        String sGender = idCard.substring(index + 2, index + 3);
        if (StringUtils.isNumeric(sGender)) {
            Integer gender = Integer.valueOf(idCard.substring(index + 2, index + 3));
            if ((gender & 1) == 1) {
                info.setMale(true);
            } else {
                info.setMale(false);
            }
        }

        //校验18位的身份证
        if (check && idCard.length() == 18) {
            int cardSum = 0;
            char[] cardChars = idCard.substring(0, 17).toCharArray();
            for (int i = 0; i < 17; i++) {
                cardSum += Integer.parseInt(String.valueOf(cardChars[i])) * powers[i];
            }
            int checkCode = (12 - cardSum % 11) % 11;
            String sCheckCode = checkCode != 10 ? checkCode + "" : "X";
            if (!sCheckCode.equalsIgnoreCase(idCard.substring(17))) {
                return null;
            }
        }
        return info;
    }

    private static DistrictInfo districtInfo(String code) {
        if (districtInfoMap.containsKey(code)) {
            return districtInfoMap.get(code);
        }

        DistrictInfo districtInfo = new DistrictInfo();
        code = code.substring(0, 2);
        if (NumberUtils.isDigits(code) && provinces.containsKey(code)) {
            districtInfo.setProvince(provinces.get(code));
        }

        return districtInfo;
    }

    public static void main(String[] args) throws Exception {
        /*Pattern pattern = Pattern.compile("[^省市县区州]+(省|市|县|区|州)");
        StringBuffer buf = new StringBuffer("");
        for (String s : BaseFileUtils.fileToString("test.txt").split(",")) {
            String[] arr = s.split(":");
            String code = arr[0];
            if (!districtInfoMap.containsKey(code)) {
                Matcher matcher = pattern.matcher(arr[1]);
                if (matcher.find()) {
                    buf.append(code).append("={\"province\":\"").append(matcher.group()).append("\"");
                }
                if (matcher.find()) {
                    buf.append(",\"city\":\"").append(matcher.group()).append("\"");
                }
                if (matcher.find()) {
                    buf.append(",\"county\":\"").append(matcher.group()).append("\"");
                }

                buf.append("}\n");
            }
        }
        System.out.println(buf.toString());*/

        //从文件中解析出新的数据
//       Pattern pattern = Pattern.compile("IdCardLocation\\(.*city=([^,]+).*county=([^)]+)\\),(\\d+)");
//        StringBuilder buf = new StringBuilder();
//        IOUtils.readLines(BaseIdCardUtils.class.getClassLoader().getResourceAsStream("zfq02"), Charsets.UTF_8).forEach(line -> {
//            Matcher matcher = pattern.matcher(line);
//            if (matcher.find()) {
//                String code = matcher.group(3).substring(0,6);
//                if(!districtInfoMap.containsKey(code)) {
//                    buf.append(code).append("={\"province\":\"").append(provinces.get(code.substring(0, 2)))
//                            .append("\",\"city\":\"").append(matcher.group(1))
//                            .append("\",\"county\":\"").append(matcher.group(2)).append("\"}\n");
//
//                    districtInfoMap.put(code, null);
//                }
//            }
//        });
//        System.out.println(buf.toString());


        //对所有数据重新排序输出
        StringBuilder buf = new StringBuilder();
        List<String> keys = new ArrayList<>(districtInfoMap.keySet());
        Collections.sort(keys);
        keys.forEach(key -> {
            DistrictInfo info = districtInfoMap.get(key);
            buf.append(key).append("={\"province\":\"").append(info.getProvince()).append("\"");
            if(StringUtils.isNotBlank(info.getCity()) && !"null".equals(info.getCity())) {
                buf.append(",\"city\":\"").append(info.getCity()).append("\"");
            } else {
                buf.append(",\"city\":\"").append(info.getProvince().replaceFirst("省$", "")).append("\"");
            }
            if(StringUtils.isNotBlank(info.getCounty())) {
                buf.append(",\"county\":\"").append(info.getCounty()).append("\"");
            }
            buf.append("}\n");
        });
        System.out.println(buf.toString());
    }
}
