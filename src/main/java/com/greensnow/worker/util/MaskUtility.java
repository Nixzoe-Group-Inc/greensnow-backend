package com.greensnow.worker.util;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MaskUtility {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();

    private static final String MASK = "***";

    @SuppressWarnings("unchecked")
    public static String toMaskedJson(Object object, String... fields) {
        try {
            Map<String, Object> values = MAPPER.convertValue(object, Map.class);
            mask(values, Set.of(fields));
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(values);
        } catch (Exception e) {
            log.error("An exception occurred when masking object to json ", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private static void mask(Map<String, Object> values, Set<String> fields) {
        values.replaceAll((field, value) -> fields.contains(field) ? MASK : value);
        values.values().forEach(value -> {
            if (value instanceof Map<?, ?> nested) {
                mask((Map<String, Object>) nested, fields);
            } else if (value instanceof Collection<?> items) {
                items.stream()
                        .filter(Map.class::isInstance)
                        .forEach(item -> mask((Map<String, Object>) item, fields));
            }
        });
    }
}
