package dev.fembyte.mirage.command.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MirageCommand {
    String name();

    String parent() default "";

    String description() default "";

    String usage() default "";

    String permission() default "";

    String[] aliases() default {};

    String[] completions() default {};

    int order() default 0;
}
