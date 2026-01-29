package dev.fembyte.mirage.config.validation;

import dev.fembyte.mirage.config.annotations.Validate;

import java.util.Collection;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static ValidationResult validateAnnotation(Object value, Validate validate) {
        if (validate == null) {
            return ValidationResult.ok();
        }
        if (value == null) {
            return ValidationResult.ok();
        }
        if (value instanceof Number number) {
            double val = number.doubleValue();
            if (val < validate.min()) {
                return ValidationResult.fail("value is below minimum " + validate.min());
            }
            if (val > validate.max()) {
                return ValidationResult.fail("value is above maximum " + validate.max());
            }
        }
        if (value instanceof String string) {
            int length = string.length();
            if (validate.minLength() >= 0 && length < validate.minLength()) {
                return ValidationResult.fail("length is below minimum " + validate.minLength());
            }
            if (validate.maxLength() >= 0 && length > validate.maxLength()) {
                return ValidationResult.fail("length is above maximum " + validate.maxLength());
            }
            if (!validate.regex().isEmpty() && !Pattern.matches(validate.regex(), string)) {
                return ValidationResult.fail("value does not match regex " + validate.regex());
            }
        }
        if (value instanceof Collection<?> collection) {
            int size = collection.size();
            if (validate.minSize() >= 0 && size < validate.minSize()) {
                return ValidationResult.fail("size is below minimum " + validate.minSize());
            }
            if (validate.maxSize() >= 0 && size > validate.maxSize()) {
                return ValidationResult.fail("size is above maximum " + validate.maxSize());
            }
        }
        return ValidationResult.ok();
    }
}
