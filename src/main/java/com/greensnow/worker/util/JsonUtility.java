package com.greensnow.worker.util;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class JsonUtility {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();

    public static String toJson(Object object) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            log.error("An exception occurred when serialising object to json ", e);
            return "";
        }
    }

    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return MAPPER.readValue(json, valueType);
        } catch (Exception e) {
            log.error("An exception occurred when deserialising json to {} ", valueType.getSimpleName(), e);
            return null;
        }
    }
}
