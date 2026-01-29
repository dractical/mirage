package dev.fembyte.mirage.config;

import dev.fembyte.mirage.config.annotations.Validate;
import dev.fembyte.mirage.config.annotations.ValidateWith;
import dev.fembyte.mirage.config.validation.ConfigValidator;
import java.lang.reflect.Field;
import java.util.List;

public record ConfigFieldInfo(
    Field field,
    String key,
    List<String> comments,
    Validate validate,
    ValidateWith validateWith
) {
    public ConfigValidator<?> newValidator() {
        if (validateWith == null) {
            return null;
        }
        Class<? extends ConfigValidator<?>> type = validateWith.value();
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create validator " + type.getName(), ex);
        }
    }
}
