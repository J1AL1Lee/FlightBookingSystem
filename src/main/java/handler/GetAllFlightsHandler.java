package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.FlightDao;
import model.Flight;
import server.SimpleHttpServer;
import utils.JsonUtil;

import java.io.IOException;
import java.util.List;

public class GetAllFlightsHandler implements HttpHandler {
    private final FlightDao flightDao = new FlightDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, "仅支持GET请求");
            return;
        }

        try {
            List<Flight> flights = flightDao.findAll();
            SimpleHttpServer.sendJsonResponse(exchange, 200, flights);
        } catch (Exception e) {
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, "查询失败：" + e.getMessage());
        }
    }
}
