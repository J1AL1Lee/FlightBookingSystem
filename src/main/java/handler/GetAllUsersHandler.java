package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDao;
import model.User;
import server.SimpleHttpServer;
import utils.JsonUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GetAllUsersHandler implements HttpHandler {
    private final UserDao userDao = new UserDao();
    //System.out.("[DEBUG] /api/admin/users/all 请求已到达");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);
        System.out.println("[DEBUG] 接收到用户列表请求");

        try {
            List<User> users = userDao.findAll();
            System.out.println("[DEBUG] 查询成功，数量：" + users.size());
            SimpleHttpServer.sendJsonResponse(exchange, 200, users);
        } catch (Exception e) {
            e.printStackTrace();  // 打印具体错误
            SimpleHttpServer.sendJsonResponse(exchange, 500, "查询失败：" + e.getMessage());
        }
    }

}
