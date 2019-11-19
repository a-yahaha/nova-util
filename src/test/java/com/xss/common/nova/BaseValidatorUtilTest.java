package com.xss.common.nova;

import com.xss.common.nova.util.BaseValidatorUtil;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.junit.Test;

import javax.validation.ValidationException;
import javax.validation.constraints.Min;

import static org.junit.Assert.assertTrue;

public class BaseValidatorUtilTest {
    @Test
    public void testBeanValidate() {
        try {
            Bean bean = new Bean();
            bean.setF2(1);
            BaseValidatorUtil.validate(bean);
            assertTrue(false);
        } catch (Throwable e) {
            assertTrue(e instanceof ValidationException);
            System.err.println(e.getMessage());
            assertTrue(e.getMessage().contains("不能为空"));
            assertTrue(e.getMessage().contains("小于5"));
        }
    }

    @Data
    public static class Bean {
        @NotBlank(message = "不能为空")
        private String f1;
        @Min(value = 5, message = "不能小于5")
        private Integer f2;
    }
}
