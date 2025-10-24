package com.ads.helpers;

import java.util.Map;

public class ServiceRequestHelper {

    // Helpers para conversión de tipos
    public static long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        return 0L;
    }

    public static double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        return 0.0;
    }

    public static float getFloatValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Float) return (Float) value;
        if (value instanceof Double) return ((Double) value).floatValue();
        if (value instanceof Long) return ((Long) value).floatValue();
        if (value instanceof Integer) return ((Integer) value).floatValue();
        return 0.0f;
    }

    public static boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }
}
