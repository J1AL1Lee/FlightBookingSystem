package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.FlightDao;
import model.Flight;
import server.SimpleHttpServer;
import utils.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FindFlightHandler implements HttpHandler {
    private final FlightDao flightDao = new FlightDao();

    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, "仅支持POST请求");
            return;
        }

        String body = SimpleHttpServer.readRequestBody(exchange);
        Map<String, Object> request = JsonUtil.fromJsonToMap(body);
        String flightId = (String) request.get("flightId");
        try {
            Flight flights = flightDao.findById(flightId);
            SimpleHttpServer.sendJsonResponse(exchange, 200, flights);
        } catch (Exception e) {
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, "查询失败：" + e.getMessage());
        }
    }
}
