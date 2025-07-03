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

        String body = SimpleHttpServer.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.fromJsonToMap(body);

        String userId = (String) data.get("userId");
        int newAuthority = Integer.parseInt(data.get("newAuthority").toString());
        //System.out.println(userId);
        //System.out.println(newAuthority);
        boolean updated = userDao.updateUserAuthority(userId, newAuthority);

        if (updated) {
            SimpleHttpServer.sendJsonResponse(exchange, 200, Map.of("success", true, "message", "权限修改成功"));
        } else {
            SimpleHttpServer.sendJsonResponse(exchange, 400, Map.of("success", false, "message", "修改失败"));
        }
    }
}
