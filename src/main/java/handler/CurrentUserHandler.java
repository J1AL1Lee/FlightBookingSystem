package handler;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CurrentUserHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        // 处理OPTIONS请求
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        // 只允许GET请求
        if (!"GET".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405,
                    SimpleHttpServer.createErrorResponse("只支持GET请求"));
            return;
        }

        try {
            // 获取session ID
            String sessionId = getSessionId(exchange);

            if (sessionId != null && LoginHandler.sessions.containsKey(sessionId)) {
                // session存在且有效
                Map<String, Object> userSession = LoginHandler.sessions.get(sessionId);

                // 检查session是否过期（可选，比如24小时过期）
                Long loginTime = (Long) userSession.get("loginTime");
                if (loginTime != null) {
                    long currentTime = System.currentTimeMillis();
                    long sessionTimeout = 24 * 60 * 60 * 1000; // 24小时

                    if (currentTime - loginTime > sessionTimeout) {
                        // session过期，移除并返回未登录
                        LoginHandler.sessions.remove(sessionId);
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "登录已过期");
                        SimpleHttpServer.sendJsonResponse(exchange, 401, response);
                        return;
                    }
                }

                // 构造返回的用户信息（不包含密码等敏感信息）
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", userSession.get("userId"));
                userInfo.put("userName", userSession.get("userName"));
                userInfo.put("userAuthority", userSession.get("userAuthority"));
                userInfo.put("vipState", userSession.get("vipState"));
                userInfo.put("userGender", userSession.get("userGender"));
                userInfo.put("userTelephone", userSession.get("userTelephone"));

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userInfo", userInfo);

                SimpleHttpServer.sendJsonResponse(exchange, 200, response);
            } else {
                // session不存在或无效
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "未登录");

                SimpleHttpServer.sendJsonResponse(exchange, 401, response);
            }

        } catch (Exception e) {
            System.err.println("❌ 获取当前用户信息失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500,
                    SimpleHttpServer.createErrorResponse("服务器内部错误"));
        }
    }

    /**
     * 从请求头中获取Session ID
     */
    private String getSessionId(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            String[] cookies = cookie.split(";");
            for (String c : cookies) {
                String[] parts = c.trim().split("=");
                if (parts.length == 2 && "SESSIONID".equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }
}