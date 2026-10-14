package com.project_shopping.shopee.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public final class WholeVndAmountValidator implements ConstraintValidator<WholeVndAmount, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value == null || value.stripTrailingZeros().scale() <= 0;
    }
}
