package dev.fembyte.mirage.config.io;

import dev.fembyte.mirage.config.ConfigFieldInfo;
import dev.fembyte.mirage.config.ConfigModuleInfo;
import dev.fembyte.mirage.config.annotations.Comment;
import dev.fembyte.mirage.config.annotations.Experimental;
import dev.fembyte.mirage.config.util.KebabCase;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class YamlConfigWriter {
    private static final String INDENT = "  ";

    public String write(int version, List<ConfigModuleInfo> modules) {
        StringBuilder out = new StringBuilder();
        out.append("config-version: ").append(version).append('\n');

        String lastCategory = null;
        boolean firstModule = true;
        for (ConfigModuleInfo module : modules) {
            String category = module.category();
            if (category != null && !category.isEmpty()) {
                if (!category.equals(lastCategory)) {
                    if (!firstModule) {
                        out.append('\n');
                    }
                    out.append(category).append(':').append('\n');
                    lastCategory = category;
                }
                writeModule(out, module, 1);
            } else {
                if (!firstModule) {
                    out.append('\n');
                }
                writeModule(out, module, 0);
            }
            firstModule = false;
        }
        return out.toString();
    }

    private void writeModule(StringBuilder out, ConfigModuleInfo module, int indentLevel) {
        Comment comment = module.type().getAnnotation(Comment.class);
        if (comment != null) {
            writeComments(out, comment.value(), indentLevel);
        }
        writeIndent(out, indentLevel);
        out.append(module.name()).append(':').append('\n');
        for (ConfigFieldInfo fieldInfo : module.fields()) {
            Field field = fieldInfo.field();
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(module.instance());
            } catch (IllegalAccessException ex) {
                continue;
            }
            int childIndent = indentLevel + 1;
            if (!fieldInfo.comments().isEmpty()) {
                writeComments(out, fieldInfo.comments(), childIndent);
            }
            if (field.isAnnotationPresent(Experimental.class)) {
                writeComments(out, new String[]{"Experimental: may change or be removed."}, childIndent);
            }
            String key = fieldInfo.key();
            writeValue(out, key, value, childIndent);
        }
    }

    private void writeValue(StringBuilder out, String key, Object value, int indentLevel) {
        if (value == null) {
            writeIndent(out, indentLevel);
            out.append(key).append(": null").append('\n');
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                writeIndent(out, indentLevel);
                out.append(key).append(": {}").append('\n');
                return;
            }
            writeIndent(out, indentLevel);
            out.append(key).append(':').append('\n');
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String entryKey = String.valueOf(entry.getKey());
                writeValue(out, entryKey, entry.getValue(), indentLevel + 1);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                writeIndent(out, indentLevel);
                out.append(key).append(": []").append('\n');
                return;
            }
            writeIndent(out, indentLevel);
            out.append(key).append(':').append('\n');
            for (Object item : collection) {
                writeIndent(out, indentLevel + 1);
                out.append("- ").append(formatScalar(item)).append('\n');
            }
            return;
        }
        writeIndent(out, indentLevel);
        out.append(key).append(": ").append(formatScalar(value)).append('\n');
    }

    private String formatScalar(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Enum<?> enumValue) {
            return KebabCase.normalize(enumValue.name());
        }
        if (value instanceof String string) {
            return quoteString(string);
        }
        return String.valueOf(value);
    }

    private String quoteString(String value) {
        if (value.isEmpty()) {
            return "''";
        }
        boolean safe = value.matches("[A-Za-z0-9_./-]+")
                && !value.contains(":")
                && !value.startsWith("-")
                && !value.startsWith("?")
                && !value.startsWith("#");
        if (safe) {
            return value;
        }
        String escaped = value.replace("'", "''");
        return "'" + escaped + "'";
    }

    private void writeIndent(StringBuilder out, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            out.append(INDENT);
        }
    }

    private void writeComments(StringBuilder out, String[] lines, int indentLevel) {
        for (String line : lines) {
            writeIndent(out, indentLevel);
            out.append("# ").append(line).append('\n');
        }
    }

    private void writeComments(StringBuilder out, List<String> lines, int indentLevel) {
        for (String line : lines) {
            writeIndent(out, indentLevel);
            out.append("# ").append(line).append('\n');
        }
    }
}
