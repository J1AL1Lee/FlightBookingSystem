package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDao;
import server.SimpleHttpServer;
import utils.JsonUtil;

import java.io.IOException;
import java.util.Map;

public class ModifyUserAuthorityHandler implements HttpHandler {

    private final UserDao userDao = new UserDao();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, Map.of("success", false, "message", "只支持POST"));
            return;
        }

        try {
            String body = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("请求体: " + body);  // ✅ 输出请求内容

            Map<String, Object> data = JsonUtil.fromJsonToMap(body);
            String userId = (String) data.get("userId");
            int newAuthority = ((Double) data.get("newAuthority")).intValue();

            System.out.println("修改用户权限 userId=" + userId + " → 权限=" + newAuthority);

            boolean updated = userDao.updateUserAuthority(userId, newAuthority);
            System.out.println("更新结果：" + updated);

            if (updated) {
                SimpleHttpServer.sendJsonResponse(exchange, 200, Map.of("success", true, "message", "权限修改成功"));
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 400, Map.of("success", false, "message", "权限修改失败"));
            }
        } catch (Exception e) {
            e.printStackTrace();  // ✅ 打印异常
            SimpleHttpServer.sendJsonResponse(exchange, 500, Map.of("success", false, "message", "服务器内部异常：" + e.getMessage()));
        }
    }

}
