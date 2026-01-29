package dev.fembyte.mirage.config;

import dev.fembyte.mirage.config.annotations.*;
import dev.fembyte.mirage.config.util.KebabCase;
import dev.fembyte.mirage.config.util.ReflectionUtil;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConfigRegistry {
    private final List<ConfigModuleInfo> modules;

    private ConfigRegistry(List<ConfigModuleInfo> modules) {
        this.modules = modules;
    }

    public static ConfigRegistry scan(String basePackage) {
        List<ConfigModuleInfo> modules = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(basePackage)
                .scan()) {
            ClassInfoList classes = scanResult.getClassesWithAnnotation(ConfigSpec.class.getName());
            for (ClassInfo classInfo : classes) {
                Class<?> type = classInfo.loadClass();
                if (!ConfigModule.class.isAssignableFrom(type)) {
                    continue;
                }
                ConfigSpec annotation = type.getAnnotation(ConfigSpec.class);
                String name = annotation.name().isEmpty()
                        ? KebabCase.fromCamel(type.getSimpleName())
                        : annotation.name();
                String category = annotation.category().isEmpty()
                        ? ""
                        : KebabCase.fromCamel(annotation.category());
                ConfigModule instance = (ConfigModule) ReflectionUtil.newInstance(type);
                ConfigModule defaults = (ConfigModule) ReflectionUtil.newInstance(type);
                List<ConfigFieldInfo> fields = collectFields(type);
                modules.add(new ConfigModuleInfo(
                        type,
                        instance,
                        defaults,
                        name,
                        category,
                        annotation.reloadable(),
                        annotation.order(),
                        fields
                ));
            }
        }
        modules.sort(Comparator.comparing(ConfigModuleInfo::category, Comparator.nullsFirst(String::compareTo))
                .thenComparingInt(ConfigModuleInfo::order)
                .thenComparing(ConfigModuleInfo::name));
        return new ConfigRegistry(modules);
    }

    public List<ConfigModuleInfo> modules() {
        return modules;
    }

    private static List<ConfigFieldInfo> collectFields(Class<?> type) {
        List<ConfigFieldInfo> fields = new ArrayList<>();
        for (Field field : ReflectionUtil.getAllFields(type)) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            String key = field.isAnnotationPresent(ConfigKey.class)
                    ? field.getAnnotation(ConfigKey.class).value()
                    : KebabCase.fromCamel(field.getName());
            Comment comment = field.getAnnotation(Comment.class);
            List<String> comments = comment == null ? List.of() : List.of(comment.value());
            Validate validate = field.getAnnotation(Validate.class);
            ValidateWith validateWith = field.getAnnotation(ValidateWith.class);
            fields.add(new ConfigFieldInfo(field, key, comments, validate, validateWith));
        }
        fields.sort(Comparator.comparing(ConfigFieldInfo::key));
        return fields;
    }
}
