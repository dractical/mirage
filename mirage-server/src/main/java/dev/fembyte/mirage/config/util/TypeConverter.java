package dev.fembyte.mirage.config.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public final class TypeConverter {
    private TypeConverter() {
    }

    public static Object convert(Object raw, Class<?> targetType, Type genericType) {
        if (raw == null) {
            return null;
        }
        if (targetType.isInstance(raw)) {
            return raw;
        }
        if (targetType == String.class) {
            return String.valueOf(raw);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return toNumber(raw).intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return toNumber(raw).longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return toNumber(raw).doubleValue();
        }
        if (targetType == float.class || targetType == Float.class) {
            return toNumber(raw).floatValue();
        }
        if (targetType == short.class || targetType == Short.class) {
            return toNumber(raw).shortValue();
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return toNumber(raw).byteValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return toBoolean(raw);
        }
        if (targetType.isEnum()) {
            return toEnum(raw, targetType);
        }
        if (List.class.isAssignableFrom(targetType)) {
            return toList(raw, genericType);
        }
        if (Set.class.isAssignableFrom(targetType)) {
            return toSet(raw, genericType);
        }
        if (Map.class.isAssignableFrom(targetType)) {
            return toMap(raw, genericType);
        }
        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }

    private static Number toNumber(Object raw) {
        if (raw instanceof Number number) {
            return number;
        }
        if (raw instanceof String str) {
            return Double.parseDouble(str.trim());
        }
        throw new IllegalArgumentException("Expected number but was " + raw + " (" + raw.getClass().getSimpleName() + ")");
    }

    private static Boolean toBoolean(Object raw) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof String str) {
            return Boolean.parseBoolean(str.trim());
        }
        if (raw instanceof Number number) {
            return number.intValue() != 0;
        }
        throw new IllegalArgumentException("Expected boolean but was " + raw + " (" + raw.getClass().getSimpleName() + ")");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object toEnum(Object raw, Class<?> targetType) {
        if (raw instanceof Enum<?>) {
            return raw;
        }
        String name = String.valueOf(raw).trim();
        String normalized = KebabCase.normalize(name).replace('-', '_');
        Object[] constants = targetType.getEnumConstants();
        for (Object constant : constants) {
            String enumName = ((Enum) constant).name();
            if (enumName.equalsIgnoreCase(name) || enumName.equalsIgnoreCase(normalized)) {
                return constant;
            }
        }
        StringBuilder valid = new StringBuilder();
        for (Object constant : constants) {
            if (!valid.isEmpty()) {
                valid.append(", ");
            }
            valid.append(KebabCase.normalize(((Enum) constant).name()));
        }
        throw new IllegalArgumentException("Unknown enum value '" + name + "' for " + targetType.getName() + ". Valid values: " + valid);
    }

    private static List<?> toList(Object raw, Type genericType) {
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected list but was " + raw.getClass().getName());
        }
        Type valueType = Object.class;
        if (genericType instanceof ParameterizedType parameterized) {
            valueType = parameterized.getActualTypeArguments()[0];
        }
        Class<?> valueClass = valueType instanceof Class<?> cls ? cls : Object.class;
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(convert(item, valueClass, valueType));
        }
        return result;
    }

    private static Set<?> toSet(Object raw, Type genericType) {
        List<?> list = toList(raw, genericType);
        return new LinkedHashSet<>(list);
    }

    private static Map<?, ?> toMap(Object raw, Type genericType) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected map but was " + raw.getClass().getName());
        }
        Type valueType = Object.class;
        if (genericType instanceof ParameterizedType parameterized) {
            Type[] args = parameterized.getActualTypeArguments();
            if (args.length > 1) {
                valueType = args[1];
            }
        }
        Class<?> valueClass = valueType instanceof Class<?> cls ? cls : Object.class;
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = convert(entry.getValue(), valueClass, valueType);
            result.put(key, value);
        }
        return result;
    }
}
