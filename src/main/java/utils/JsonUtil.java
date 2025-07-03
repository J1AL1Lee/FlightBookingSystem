package utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JSON 工具类，用于对象与 JSON 字符串之间的转换。
 * 使用 Gson 进行处理，支持自定义时间格式与 UTF-8 编码，
 * 并扩展支持 java.time.LocalTime 类型的序列化与反序列化。
 */
public class JsonUtil {

    // Gson 实例（注册 LocalTime 的适配器）
    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss") // 设置默认时间格式（用于 Date 类型）
                .disableHtmlEscaping(); // 禁用 HTML 转义，避免中文被转义

        // 注册 LocalTime 的序列化器：将 LocalTime 转为 "HH:mm" 字符串
        builder.registerTypeAdapter(LocalTime.class, new JsonSerializer<LocalTime>() {
            @Override
            public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        });

        // 注册 LocalTime 的反序列化器：从 "HH:mm" 字符串解析为 LocalTime 对象
        builder.registerTypeAdapter(LocalTime.class, new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return LocalTime.parse(json.getAsString(), DateTimeFormatter.ofPattern("HH:mm"));
            }
        });

        gson = builder.create();
    }

    // 对象转JSON，确保UTF-8编码
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        String json = gson.toJson(obj);
        return new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    // JSON转对象，确保UTF-8解码
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String utf8Json = new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return gson.fromJson(utf8Json, clazz);
    }

    // JSON转对象（泛型版本）
    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String utf8Json = new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return gson.fromJson(utf8Json, type);
    }

    // JSON转Map - 修复类型问题
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        String utf8Json = new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return gson.fromJson(utf8Json, type);
    }
}
