package dev.fembyte.mirage.config.annotations;

import dev.fembyte.mirage.config.validation.ConfigValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidateWith {
    Class<? extends ConfigValidator<?>> value();
}
