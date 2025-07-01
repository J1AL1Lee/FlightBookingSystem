package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDao;
import model.User;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersHandler implements HttpHandler {
    private UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if (!"GET".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, SimpleHttpServer.createErrorResponse("只支持GET请求"));
            return;
        }

        try {
            List<User> users = userDao.findAll();

            // 不返回密码信息
            List<Map<String, Object>> userList = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", user.getUserId());
                userInfo.put("userName", user.getUserName());
                userInfo.put("userGender", user.getUserGender());
                userInfo.put("userTelephone", user.getUserTelephone());
                userInfo.put("vipState", user.getVipState());
                userInfo.put("signUpTime", user.getUserSignUpTime().toString());
                userList.add(userInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userList);
            response.put("count", users.size());

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);
            System.out.println("📊 返回用户列表，共 " + users.size() + " 个用户");

        } catch (Exception e) {
            System.err.println("❌ 获取用户列表失败: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("获取用户列表失败"));
        }
    }
}
