package edu.ump.timetabling.util;

import java.util.*;

/**
 * Lightweight, zero-dependency JSON serializer and parser for Java 11+.
 */
public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List || obj instanceof Object[]) {
            Iterable<?> list = (obj instanceof Object[]) ? Arrays.asList((Object[]) obj) : (Iterable<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof int[]) {
            int[] arr = (int[]) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(arr[i]);
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof double[]) {
            double[] arr = (double[]) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(arr[i]);
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - hex.length(); k++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Simple JSON parser for flat key-value / nested objects.
     */
    public static Map<String, Object> parseJsonObject(String jsonStr) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) return result;
        String trimmed = jsonStr.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        List<String> tokens = splitTopLevel(trimmed, ',');
        for (String token : tokens) {
            int colonIdx = token.indexOf(':');
            if (colonIdx > 0) {
                String key = token.substring(0, colonIdx).trim();
                String val = token.substring(colonIdx + 1).trim();
                key = unquote(key);
                result.put(key, parseValue(val));
            }
        }
        return result;
    }

    private static Object parseValue(String val) {
        if (val == null || val.isEmpty() || "null".equalsIgnoreCase(val)) return null;
        if ("true".equalsIgnoreCase(val)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(val)) return Boolean.FALSE;
        if (val.startsWith("\"") && val.endsWith("\"")) {
            return unquote(val);
        }
        // Parse JSON arrays: [item1, item2, ...]
        if (val.startsWith("[") && val.endsWith("]")) {
            String inner = val.substring(1, val.length() - 1).trim();
            List<Object> list = new ArrayList<>();
            if (!inner.isEmpty()) {
                for (String item : splitTopLevel(inner, ',')) {
                    list.add(parseValue(item.trim()));
                }
            }
            return list;
        }
        // Parse nested JSON objects: { ... }
        if (val.startsWith("{") && val.endsWith("}")) {
            return parseJsonObject(val);
        }
        try {
            if (val.contains(".")) {
                return Double.parseDouble(val);
            } else {
                return Integer.parseInt(val);
            }
        } catch (NumberFormatException e) {
            return val;
        }
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1)
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return s;
    }

    private static List<String> splitTopLevel(String s, char delimiter) {
        List<String> list = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == delimiter && depth == 0) {
                    if (current.length() > 0) {
                        list.add(current.toString().trim());
                        current.setLength(0);
                    }
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            list.add(current.toString().trim());
        }
        return list;
    }
}
