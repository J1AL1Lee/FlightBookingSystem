package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.DatabaseConnection;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class TestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response;
        try {
            DatabaseConnection.testConnection();
            response = JsonUtil.toJson(Map.of(
                    "status", "success",
                    "message", "数据库连接正常",
                    "timestamp", new Date().toString()
            ));
        } catch (Exception e) {
            response = JsonUtil.toJson(SimpleHttpServer.createErrorResponse("数据库连接失败: " + e.getMessage()));
        }

        SimpleHttpServer.sendJsonResponse(exchange, 200, response);
    }
}
