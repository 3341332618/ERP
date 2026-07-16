package com.erp.service;

import java.util.LinkedHashMap;
import java.util.Map;

final class ServicePayloads {
    private ServicePayloads() {
    }

    static Map<String, String> stringMap(Map<String, ?> payload) {
        var result = new LinkedHashMap<String, String>();
        if (payload == null) {
            return result;
        }
        payload.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    static Map<String, Object> objectMap(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }
}
