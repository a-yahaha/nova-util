package com.xss.common.nova.validator;

import com.xss.common.nova.annotation.IdCard;
import com.xss.common.nova.util.BaseIdCardUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IdCardValidator implements ConstraintValidator<IdCard, String> {
    @Override
    public void initialize(IdCard idCard) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isNotBlank(s) && BaseIdCardUtils.parseIdCard(s, true) == null) {
            return false;
        }
        return true;
    }
}
