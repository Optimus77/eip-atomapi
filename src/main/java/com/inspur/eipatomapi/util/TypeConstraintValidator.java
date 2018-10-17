package com.inspur.eipatomapi.util;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TypeConstraintValidator implements ConstraintValidator<TypeConstraint, String> {

    private String[] validValues;
    @Override
    public void initialize(TypeConstraint constraintAnnotation) {
        validValues = constraintAnnotation.allowedValues();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context){
        for (String s : this.validValues) {
            if (s.equals(value)) {
                return true;
            }
        } return false;
    }
}