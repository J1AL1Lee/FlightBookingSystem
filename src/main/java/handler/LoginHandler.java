package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDao;
import model.User;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, 0);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, SimpleHttpServer.createErrorResponse("只支持POST请求"));
            return;
        }

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📨 收到登录请求: " + requestBody);

            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
            String userId = (String) requestData.get("userId");
            String userPassword = (String) requestData.get("userPassword");

            if (userId == null || userPassword == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("用户ID和密码不能为空"));
                return;
            }

            User user = userDao.login(userId, userPassword);
            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "登录成功");
                response.put("userId", user.getUserId());
                response.put("userName", user.getUserName());
                response.put("vipState", user.getVipState());
                response.put("userAuthority", user.getUserAuthority());

                SimpleHttpServer.sendJsonResponse(exchange, 200, response);
                System.out.println("✅ 用户登录成功: " + user.getUserName() + " (" + user.getUserId() + ")");
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 401, SimpleHttpServer.createErrorResponse("用户ID或密码错误"));
            }

        } catch (Exception e) {
            System.err.println("❌ 登录失败: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("登录失败: " + e.getMessage()));
        }
    }
}
