package utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()  // 禁用HTML转义，避免中文被转义
            .create();

    // 对象转JSON，确保UTF-8编码
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        String json = gson.toJson(obj);
        // 确保是UTF-8字符串
        return new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    // JSON转对象，确保UTF-8解码
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        // 确保输入是UTF-8字符串
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