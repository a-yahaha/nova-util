package com.xss.common.nova.util;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;

public class BaseValidatorUtil {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public static void validate(Object obj) {
        if (obj == null) {
            return;
        }

        StringBuilder errors = new StringBuilder();
        for (ConstraintViolation violation : validator.validate(obj)) {
            ConstraintViolationImpl violationImp = (ConstraintViolationImpl) violation;
            errors.append("[")
                    .append(violationImp.getPropertyPath().toString()).append(":")
                    .append(violationImp.getMessage()).append("]");
        }

        if (errors.length() > 0) {
            throw new ValidationException(String.format("[%s]对象不符合规则:%s", obj.getClass().getSimpleName(), errors.toString()));
        }
    }
}
