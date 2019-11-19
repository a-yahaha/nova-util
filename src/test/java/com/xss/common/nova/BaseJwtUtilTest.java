package com.xss.common.nova;

import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.util.BaseJwtUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class BaseJwtUtilTest {
    @Test
    public void test() {
        String secret = "secret123";
        Map<String, Object> content = ImmutableMap.of("type", "service", "version", 1, "key", "value");
        String token = BaseJwtUtil.getToken(content, null, 10, secret).getToken();

        Map<String, Object> tokenInfo = BaseJwtUtil.getTokenInfo(token);
        Assert.assertTrue(tokenInfo.containsKey(BaseJwtUtil.CONTENT));
        Assert.assertEquals(((Map) tokenInfo.get(BaseJwtUtil.CONTENT)).get("key"), "value");

        content = BaseJwtUtil.verifyToken(token, secret, Map.class);
        Assert.assertEquals(content.get("key"), "value");

        content = BaseJwtUtil.verifyToken("Bearer " + token, secret, Map.class);
        Assert.assertEquals(content.get("key"), "value");
    }
}

