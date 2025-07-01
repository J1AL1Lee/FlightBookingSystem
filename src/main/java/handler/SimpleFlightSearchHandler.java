package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.FlightSearchService;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleFlightSearchHandler implements HttpHandler {
    private FlightSearchService searchService = new FlightSearchService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, SimpleHttpServer.createErrorResponse("只支持POST请求"));
            return;
        }

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("🔍 收到航班搜索请求: " + requestBody);

            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);

            // 获取基本参数
            String airportFrom = (String) requestData.get("airportFrom");
            String airportTo = (String) requestData.get("airportTo");
            String flightDate = (String) requestData.get("flightDate");
            String userId = (String) requestData.get("userId"); // 可选，用于VIP判断

            // 参数验证
            if (airportFrom == null || airportTo == null || flightDate == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：airportFrom, airportTo, flightDate"));
                return;
            }

            if (airportFrom.equals(airportTo)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("出发机场和到达机场不能相同"));
                return;
            }

            try {
                LocalDate date = LocalDate.parse(flightDate);

                // 调用搜索服务
                List<FlightSearchService.FlightSearchResult> results =
                        searchService.searchAvailableFlights(airportFrom, airportTo, date, userId);

                // 转换为前端友好的格式
                List<Map<String, Object>> flightList = new ArrayList<>();
                for (FlightSearchService.FlightSearchResult result : results) {
                    Map<String, Object> flightData = new HashMap<>();

                    // 基本信息
                    flightData.put("flightId", result.getFlightId());
                    flightData.put("airportFrom", result.getAirportFrom());
                    flightData.put("airportTo", result.getAirportTo());
                    flightData.put("flightDate", result.getFlightDate().toString());
                    flightData.put("timeTakeoff", result.getTimeTakeoff().toString());
                    flightData.put("timeArrive", result.getTimeArrive().toString());

                    // 航空公司信息
                    flightData.put("airlineName", result.getAirlineName());

                    // 座位信息
                    flightData.put("seat0Left", result.getSeat0Left());
                    flightData.put("seat1Left", result.getSeat1Left());

                    // 价格信息（根据VIP状态显示）
                    flightData.put("finalPrice0", result.getFinalPrice0());
                    flightData.put("finalPrice1", result.getFinalPrice1());
                    flightData.put("isVipUser", result.isVipUser());

                    // 如果是VIP用户且有折扣，显示折扣信息
                    if (result.isVipUser() && result.getDiscount() < 1.0f) {
                        flightData.put("originalPrice0", result.getOriginalPrice0());
                        flightData.put("originalPrice1", result.getOriginalPrice1());
                        flightData.put("discount", result.getDiscount());
                        flightData.put("hasDiscount", true);
                    } else {
                        flightData.put("hasDiscount", false);
                    }

                    // 航程记录ID（用于后续预订）
                    flightData.put("flightrecordId", result.getFlightrecord().getFlightrecordId());

                    flightList.add(flightData);
                }

                // 返回结果
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", flightList);
                response.put("count", results.size());
                response.put("message", "搜索成功，找到 " + results.size() + " 个航班");

                SimpleHttpServer.sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("日期格式错误或搜索失败: " + e.getMessage()));
            }

        } catch (Exception e) {
            System.err.println("❌ 航班搜索API处理失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("搜索失败: " + e.getMessage()));
        }
    }
}
