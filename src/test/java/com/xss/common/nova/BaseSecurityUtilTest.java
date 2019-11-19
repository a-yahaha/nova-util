package com.xss.common.nova;


import com.xss.common.nova.util.BaseSecurityUtil;
import org.apache.commons.lang.math.NumberUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class BaseSecurityUtilTest {
    @Test
    public void test() {
        //md5测试
        assertNull(BaseSecurityUtil.md5(null));
        assertEquals(BaseSecurityUtil.md5(""), "");
        assertTrue(BaseSecurityUtil.md5("test").length() > 0);

        //base64测试
        String src = "test中文";
        String des = BaseSecurityUtil.base64Encode(src);
        assertEquals(BaseSecurityUtil.base64Decode(des), src);

        //aes测试
        String secret = "secret";
        des = BaseSecurityUtil.aesEncrypt(src, secret);
        assertEquals(BaseSecurityUtil.aesDecrypt(des, secret), src);
        des = BaseSecurityUtil.aesEncrypt2(src, secret);
        assertEquals(BaseSecurityUtil.aesDecrypt2(des, secret), src);

        des = BaseSecurityUtil.aesEncrypt(src, secret);
        assertEquals(BaseSecurityUtil.aesDecrypt(des, secret), src);

        //rsa测试
        String privateKey = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBANP2KuhAwLD2pX9ainQffGaAIY46cLxZL/rIVXPbrYYDPd4HB/bSFKVjg0pQRKU2dO05d86O4dExpCHLT0+IWTzUOE5VyLit3g2W1dlKrLa6Er4IggcKLGIjp1oTNDWHk6xnLwhJdrukwoWPxGmaL0JyJzRtzj2o3Esywi2yPSZlAgMBAAECgYAC7iJFt69yQtai3hOP62eC2z6bgr9QO0NoiiB0S5MoiR7v1NUNWnYimy+TdWydhBU1ulenqV4B0Ffeh7r+9HmXUT7YpJo7g2RYZ+D0PR4pN2mCC2EAP3XV2mTGhe/kUt/GDR1OOkR0o4nrGvRF2KWHTOGJ7oU78tccWtMiOzzOOQJBAOuHWiFlbE8CG8RNh9DZbTo/jQ/5sVSvnSm9IcHWQfu8Rv8Nfy4/qKpB+xry+RB4e/TkBa6/DESIz/Js0C8xFqMCQQDmYm7944xV22/H+d/dvSclxBYo1m1QTp+dDrGeGzF+QD9K7SpJXK5jdQ5yu70ieTEueTRhfCD7erPIG2yqUAdXAkEAmm4uJ7WTtZ5BTI41XjgiCU5AFNh4cHmRSBvNtYXhz8hcmMFlwZJV6gTHO51St3z4cdLM8w4rYgh+qIq2WisKlwJAOEtpL6TAj+I06DqIl1g3DqVhfM0YnPz5R5llkWq3p0/vp1FYeeCpxCfARgzV5GyUtfRr9j4smQack5MP9HXltQJBAMhJYBOyBEsyjLi+PykeH11DXmUsPQp+tpGOjJMNXlwcIEjECe1CbJtQ1DBld/EL4u2l8kshXRkotvBM8L8hpWc=";
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDT9iroQMCw9qV/Wop0H3xmgCGOOnC8WS/6yFVz262GAz3eBwf20hSlY4NKUESlNnTtOXfOjuHRMaQhy09PiFk81DhOVci4rd4NltXZSqy2uhK+CIIHCixiI6daEzQ1h5OsZy8ISXa7pMKFj8Rpmi9Ccic0bc49qNxLMsItsj0mZQIDAQAB";
        des = BaseSecurityUtil.rsaEncryptByPrivateKey(src, privateKey);
        assertEquals(src, BaseSecurityUtil.rsaDecryptByPublicKey(des, publicKey));

        des = BaseSecurityUtil.rsaEncryptByPublicKey(src, publicKey);
        assertEquals(src, BaseSecurityUtil.rsaDecryptByPrivateKey(des, privateKey));

        src = "我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊我要用一个长一点的东西来做测试啊";
        des = BaseSecurityUtil.rsaEncryptByPrivateKey(src, privateKey);
        assertEquals(src, BaseSecurityUtil.rsaDecryptByPublicKey(des, publicKey));

        des = BaseSecurityUtil.rsaEncryptByPublicKey(src, publicKey);
        assertEquals(src, BaseSecurityUtil.rsaDecryptByPrivateKey(des, privateKey));

        //md5测试
        String md5 = BaseSecurityUtil.md5(true, src, src);
        assertTrue(NumberUtils.isDigits(md5));
    }
}
