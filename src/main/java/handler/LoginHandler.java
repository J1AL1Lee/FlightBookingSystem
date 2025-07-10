package handler;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.UserDao;
import model.User;
import server.SimpleHttpServer;
import utils.JsonUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoginHandler implements HttpHandler {
    // 静态Session存储 - 生产环境建议使用Redis或数据库
    public static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    private UserDao userDao = new UserDao();

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
            // 读取请求体
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("🔍 登录请求: " + requestBody);

            // 解析JSON
            Map<String, Object> loginData = JsonUtil.fromJsonToMap(requestBody);
            String userId = (String) loginData.get("userId");
            String userPassword = (String) loginData.get("userPassword");

            // 验证输入
            if (userId == null || userId.trim().isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400,
                        SimpleHttpServer.createErrorResponse("用户ID不能为空"));
                return;
            }

            if (userPassword == null || userPassword.trim().isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400,
                        SimpleHttpServer.createErrorResponse("密码不能为空"));
                return;
            }

            // 验证用户ID格式（6位数字）
            if (!userId.matches("^[0-9]{6}$")) {
                SimpleHttpServer.sendJsonResponse(exchange, 400,
                        SimpleHttpServer.createErrorResponse("用户ID必须是6位数字"));
                return;
            }

            // 验证用户凭据（使用你现有的login方法）
            User user = userDao.login(userId.trim(), userPassword.trim());

            if (user != null) {
                // 登录成功，创建session
                String sessionId = UUID.randomUUID().toString();

                // 存储用户信息到session
                Map<String, Object> userSession = new HashMap<>();
                userSession.put("userId", user.getUserId());
                userSession.put("userName", user.getUserName());
                userSession.put("userAuthority", user.getUserAuthority());
                userSession.put("vipState", user.getVipState());
                userSession.put("userGender", user.getUserGender());
                userSession.put("userTelephone", user.getUserTelephone());
                userSession.put("loginTime", System.currentTimeMillis());

                // 保存session
                sessions.put(sessionId, userSession);

                // 设置Cookie（HttpOnly防止XSS攻击）
                String cookieValue = String.format(
                        "SESSIONID=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
                        sessionId,
                        24 * 60 * 60 // 24小时过期
                );
                exchange.getResponseHeaders().add("Set-Cookie", cookieValue);

                // 构造响应（只返回非敏感信息）
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userName", user.getUserName());
                response.put("message", "登录成功");

                System.out.println("✅ 用户 " + userId + " 登录成功，SessionID: " + sessionId);
                SimpleHttpServer.sendJsonResponse(exchange, 200, response);

            } else {
                // 登录失败
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户名或密码错误");

                System.out.println("❌ 用户 " + userId + " 登录失败：用户名或密码错误");
                SimpleHttpServer.sendJsonResponse(exchange, 200, response);
            }

        } catch (Exception e) {
            System.err.println("❌ 登录处理失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500,
                    SimpleHttpServer.createErrorResponse("服务器内部错误"));
        }
    }

    /**
     * 清理过期的session（可以定期调用）
     */
    public static void cleanExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 24 * 60 * 60 * 1000; // 24小时

        sessions.entrySet().removeIf(entry -> {
            Map<String, Object> session = entry.getValue();
            Long loginTime = (Long) session.get("loginTime");
            return loginTime != null && (currentTime - loginTime) > sessionTimeout;
        });

        System.out.println("🧹 清理过期session，当前活跃session数量: " + sessions.size());
    }
}