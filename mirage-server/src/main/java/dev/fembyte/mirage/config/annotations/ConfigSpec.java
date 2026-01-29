package dev.fembyte.mirage.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigSpec {
    String name() default "";

    String category() default "";

    boolean reloadable() default true;

    int order() default 0;
}
