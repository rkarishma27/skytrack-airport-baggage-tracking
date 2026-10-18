package com.airport.baggage.web;

import java.math.BigDecimal;
import java.util.Set;

/** Tiny input-validation helpers used by the controllers. */
public final class Check {
    private Check() {}

    public static String required(String v, String field, int max) {
        if (v == null || v.isBlank()) throw ApiException.badRequest(field + " is required");
        return maxLen(v.trim(), field, max);
    }

    public static String optional(String v, String field, int max) {
        if (v == null || v.isBlank()) return null;
        return maxLen(v.trim(), field, max);
    }

    public static String maxLen(String v, String field, int max) {
        if (v.length() > max) throw ApiException.badRequest(field + " must be at most " + max + " characters");
        return v;
    }

    public static <T> T required(T v, String field) {
        if (v == null) throw ApiException.badRequest(field + " is required");
        return v;
    }

    public static String oneOf(String v, String field, Set<String> allowed, String defaultValue) {
        if (v == null || v.isBlank()) {
            if (defaultValue == null) throw ApiException.badRequest(field + " is required");
            return defaultValue;
        }
        if (!allowed.contains(v)) throw ApiException.badRequest(field + " must be one of " + allowed);
        return v;
    }

    public static BigDecimal weight(BigDecimal w) {
        if (w == null) return null;
        if (w.signum() < 0 || w.compareTo(new BigDecimal("999.99")) > 0)
            throw ApiException.badRequest("Weight must be between 0 and 999.99 kg");
        return w;
    }
}
