package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDao;
import model.User;
import utils.JsonUtil;
import server.SimpleHttpServer;

import java.io.IOException;
import java.util.Map;

public class UpdateUserHandler implements HttpHandler {

    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, Map.of("success", false, "message", "只支持POST请求"));
            return;
        }

        Map<String, Object> sessionUser = SimpleHttpServer.getCurrentUser(exchange);
        if (sessionUser == null) {
            SimpleHttpServer.sendUnauthorizedResponse(exchange);
            return;
        }

        String userId = (String) sessionUser.get("userId");
        String body = SimpleHttpServer.readRequestBody(exchange);
        Map<String, Object> request = JsonUtil.fromJsonToMap(body);

        String name = (String) request.get("user_name");
        String password = (String) request.get("user_password");
        String gender = (String) request.get("user_gender");
        String phone = (String) request.get("user_telephone");

        boolean success = userDao.updateUserInfo(userId, name, password, gender, phone);
        if (success) {
            SimpleHttpServer.sendJsonResponse(exchange, 200, Map.of("success", true, "message", "用户信息更新成功"));
        } else {
            SimpleHttpServer.sendJsonResponse(exchange, 500, Map.of("success", false, "message", "更新失败"));
        }
    }
}
