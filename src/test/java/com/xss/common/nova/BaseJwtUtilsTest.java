package com.xss.common.nova;

import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.util.BaseJwtUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class BaseJwtUtilsTest {
    @Test
    public void test() {
        String secret = "secret123";
        Map<String, Object> content = ImmutableMap.of("type", "service", "version", 1, "key", "value");
        String token = BaseJwtUtils.getToken(content, null, 10, secret).getToken();

        Map<String, Object> tokenInfo = BaseJwtUtils.getTokenInfo(token);
        Assert.assertTrue(tokenInfo.containsKey(BaseJwtUtils.CONTENT));
        Assert.assertEquals(((Map) tokenInfo.get(BaseJwtUtils.CONTENT)).get("key"), "value");

        content = BaseJwtUtils.verifyToken(token, secret, Map.class);
        Assert.assertEquals(content.get("key"), "value");

        content = BaseJwtUtils.verifyToken("Bearer " + token, secret, Map.class);
        Assert.assertEquals(content.get("key"), "value");
    }
}

