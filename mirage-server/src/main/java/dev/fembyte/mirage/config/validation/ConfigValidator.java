package dev.fembyte.mirage.config.validation;

public interface ConfigValidator<T> {
    ValidationResult validate(T value);
}
