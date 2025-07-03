package handler;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        // 处理OPTIONS请求
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        // 只允许POST请求
        if (!"POST".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405,
                    SimpleHttpServer.createErrorResponse("只支持POST请求"));
            return;
        }

        try {
            // 获取session ID
            String sessionId = getSessionId(exchange);

            if (sessionId != null && LoginHandler.sessions.containsKey(sessionId)) {
                // 从session存储中移除
                Map<String, Object> userSession = LoginHandler.sessions.remove(sessionId);
                String userId = (String) userSession.get("userId");
                System.out.println("✅ 用户 " + userId + " 退出登录成功");
            }

            // 清除Cookie（设置过期时间为0）
            String cookieValue = "SESSIONID=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax";
            exchange.getResponseHeaders().add("Set-Cookie", cookieValue);

            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已退出登录");

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 退出登录处理失败: " + e.getMessage());
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