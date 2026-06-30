package com.devicemind.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类 — 全项目唯一的 ObjectMapper 实例
 * <p>
 * 替代各模块各自 {@code @Autowired ObjectMapper} 或 {@code new ObjectMapper()}。
 * 静态方法直接调用，无需注入。
 * <p>
 * 配置：
 * <ul>
 *   <li>忽略未知属性（反序列化时不会因多余字段报错）</li>
 *   <li>日期/时间格式使用 ISO-8601</li>
 *   <li>Java 8 时间类型（LocalDateTime 等）支持</li>
 * </ul>
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule());

    private JsonUtil() {}

    /** 获取底层 ObjectMapper（少数需要直接用的场景） */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** 对象 → JSON 字符串 */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("序列化失败: " + obj.getClass().getSimpleName(), e);
        }
    }

    /** 对象 → JSON 字符串（不抛异常，返回 null） */
    public static String toJsonOrNull(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** JSON → 对象 */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new JsonException("反序列化失败: " + clazz.getSimpleName(), e);
        }
    }

    /** JSON → 泛型对象（如 {@code List<User>}） */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (IOException e) {
            throw new JsonException("反序列化失败", e);
        }
    }

    /** JSON → Map<String, Object> */
    public static Map<String, Object> toMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }

    /** JSON → List<Map<String, Object>> */
    public static List<Map<String, Object>> toMapList(String json) {
        return fromJson(json, new TypeReference<List<Map<String, Object>>>() {});
    }

    /** 对象 → JSON 字节数组 */
    public static byte[] toBytes(Object obj) {
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("序列化失败", e);
        }
    }

    /** JSON 字符串 → JsonNode 树 */
    public static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new JsonException("JSON 解析失败", e);
        }
    }

    /** JsonNode → Java 对象 */
    public static <T> T treeToValue(com.fasterxml.jackson.databind.JsonNode node, Class<T> clazz) {
        try {
            return MAPPER.treeToValue(node, clazz);
        } catch (IOException e) {
            throw new JsonException("treeToValue 失败", e);
        }
    }

    /** 运行时异常，避免到处 try-catch */
    public static class JsonException extends RuntimeException {
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
