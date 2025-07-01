package handler;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.FlightDao;
import model.Flight;
import server.SimpleHttpServer;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Map;

public class AddFlightHandler implements HttpHandler {
    private FlightDao flightDao = new FlightDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if (!"POST".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, Map.of("success", false, "message", "仅支持POST请求"));
            return;
        }

        try {
            String body = SimpleHttpServer.readRequestBody(exchange);
            Map<String, Object> data = utils.JsonUtil.fromJsonToMap(body);

            // 提取字段（要求前端发送完整字段）
            String flightId = (String) data.get("flightId");
            String airlineId = (String) data.get("airlinecompanyId");
            String airportFrom = (String) data.get("airportFrom");
            String airportTo = (String) data.get("airportTo");
            String timeTakeoff = (String) data.get("timeTakeoff");
            String timeArrive = (String) data.get("timeArrive");
            int seat0Capacity = Integer.parseInt(data.get("seat0Capacity").toString());
            int seat1Capacity = Integer.parseInt(data.get("seat1Capacity").toString());
            int seat0Price = Integer.parseInt(data.get("seat0Price").toString());
            int seat1Price = Integer.parseInt(data.get("seat1Price").toString());
            float discount = Float.parseFloat(data.get("discount").toString());

            // 创建航班对象
            Flight flight = new Flight();
            flight.setFlightId(flightId);
            flight.setAirlinecompanyId(airlineId);
            flight.setAirportFrom(airportFrom);
            flight.setAirportTo(airportTo);
            flight.setTimeTakeoff(LocalTime.parse(timeTakeoff));
            flight.setTimeArrive(LocalTime.parse(timeArrive));
            flight.setSeat0Capacity(seat0Capacity);
            flight.setSeat1Capacity(seat1Capacity);
            flight.setSeat0Price(seat0Price);
            flight.setSeat1Price(seat1Price);
            flight.setDiscount(discount);

            // 保存
            if (flightDao.existsById(flightId)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, Map.of("success", false, "message", "航班ID已存在"));
                return;
            }

            flightDao.save(flight);
            SimpleHttpServer.sendJsonResponse(exchange, 200, Map.of("success", true, "message", "航班添加成功"));

        } catch (Exception e) {
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, Map.of("success", false, "message", "添加失败：" + e.getMessage()));
        }
    }

}

