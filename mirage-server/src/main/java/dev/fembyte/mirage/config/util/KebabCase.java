package dev.fembyte.mirage.config.util;

public final class KebabCase {
    private KebabCase() {
    }

    public static String fromCamel(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder out = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                if (i > 0 && chars[i - 1] != '-' && chars[i - 1] != '_') {
                    out.append('-');
                }
                out.append(Character.toLowerCase(c));
            } else if (c == '_') {
                out.append('-');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replace('_', '-').toLowerCase();
    }
}
