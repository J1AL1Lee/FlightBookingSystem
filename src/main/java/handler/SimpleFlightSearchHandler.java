package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.FlightSearchService;
import model.Flight;
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

            // 检查搜索类型：按航班号搜索 vs 按航线搜索
            String flightId = (String) requestData.get("flightId");
            String userId = (String) requestData.get("userId"); // 可选，用于VIP判断

            if (flightId != null && !flightId.trim().isEmpty()) {
                // 按航班号搜索
                handleFlightIdSearch(exchange, requestData, flightId, userId);
            } else {
                // 按航线搜索（原有逻辑）
                handleRouteSearch(exchange, requestData, userId);
            }

        } catch (Exception e) {
            System.err.println("❌ 航班搜索API处理失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 处理按航班号搜索（支持模糊查询）
     */
    private void handleFlightIdSearch(HttpExchange exchange, Map<String, Object> requestData,
                                      String flightId, String userId) throws IOException {
        try {
            // 检查是否启用模糊查询
            Boolean fuzzySearch = (Boolean) requestData.get("fuzzySearch");
            boolean isFuzzy = fuzzySearch != null && fuzzySearch;

            List<Flight> matchedFlights = new ArrayList<>();

            if (isFuzzy) {
                // 使用智能搜索（推荐）
                matchedFlights = searchService.smartSearchFlightId(flightId);
                System.out.println("🧠 智能搜索航班号: " + flightId);
            } else {
                // 精确查询
                Flight flight = searchService.getFlightById(flightId);
                if (flight != null) {
                    matchedFlights.add(flight);
                }
                System.out.println("🎯 精确搜索航班号: " + flightId);
            }

            if (matchedFlights.isEmpty()) {
                String searchType = isFuzzy ? "智能搜索" : "精确搜索";
                SimpleHttpServer.sendJsonResponse(exchange, 404,
                        SimpleHttpServer.createErrorResponse(searchType + "未找到包含 \"" + flightId + "\" 的航班"));
                return;
            }

            // 获取指定日期或未来日期的航班记录
            String flightDate = (String) requestData.get("flightDate");
            List<FlightSearchService.FlightSearchResult> allResults = new ArrayList<>();

            for (Flight flight : matchedFlights) {
                List<FlightSearchService.FlightSearchResult> flightResults = new ArrayList<>();

                if (flightDate != null && !flightDate.trim().isEmpty()) {
                    // 搜索指定日期
                    try {
                        LocalDate date = LocalDate.parse(flightDate);
                        List<FlightSearchService.FlightSearchResult> dateResults =
                                searchService.searchAvailableFlights(flight.getAirportFrom(), flight.getAirportTo(), date, userId);

                        // 只返回匹配的航班号
                        for (FlightSearchService.FlightSearchResult result : dateResults) {
                            if (flight.getFlightId().equals(result.getFlightId())) {
                                flightResults.add(result);
                            }
                        }
                    } catch (Exception e) {
                        SimpleHttpServer.sendJsonResponse(exchange, 400,
                                SimpleHttpServer.createErrorResponse("日期格式错误: " + e.getMessage()));
                        return;
                    }
                } else {
                    // 搜索未来7天
                    LocalDate today = LocalDate.now();
                    for (int i = 0; i < 7; i++) {
                        LocalDate searchDate = today.plusDays(i);
                        List<FlightSearchService.FlightSearchResult> dateResults =
                                searchService.searchAvailableFlights(flight.getAirportFrom(), flight.getAirportTo(), searchDate, userId);

                        // 只返回匹配的航班号
                        for (FlightSearchService.FlightSearchResult result : dateResults) {
                            if (flight.getFlightId().equals(result.getFlightId())) {
                                flightResults.add(result);
                            }
                        }
                    }
                }

                allResults.addAll(flightResults);
            }

            // 转换为前端友好的格式
            List<Map<String, Object>> flightList = convertToFlightList(allResults);

            // 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", flightList);
            response.put("count", allResults.size());
            response.put("searchType", isFuzzy ? "flightIdFuzzy" : "flightIdExact");
            response.put("flightId", flightId);
            response.put("fuzzySearch", isFuzzy);
            response.put("matchedFlights", matchedFlights.size()); // 匹配到的航班数量

            String searchMethod = isFuzzy ? "智能搜索" : "精确搜索";
            if (flightDate != null) {
                response.put("message", searchMethod + "航班号包含 \"" + flightId + "\" 在 " + flightDate + " 的 " + allResults.size() + " 个班次");
            } else {
                response.put("message", searchMethod + "航班号包含 \"" + flightId + "\" 未来7天的 " + allResults.size() + " 个班次");
            }

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            SimpleHttpServer.sendJsonResponse(exchange, 400,
                    SimpleHttpServer.createErrorResponse("按航班号搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 处理按航线搜索（原有逻辑）
     */
    private void handleRouteSearch(HttpExchange exchange, Map<String, Object> requestData,
                                   String userId) throws IOException {
        try {
            // 获取基本参数
            String airportFrom = (String) requestData.get("airportFrom");
            String airportTo = (String) requestData.get("airportTo");
            String flightDate = (String) requestData.get("flightDate");

            // 参数验证
            if (airportFrom == null || airportTo == null || flightDate == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400,
                        SimpleHttpServer.createErrorResponse("缺少必需参数：airportFrom, airportTo, flightDate"));
                return;
            }

            if (airportFrom.equals(airportTo)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400,
                        SimpleHttpServer.createErrorResponse("出发机场和到达机场不能相同"));
                return;
            }

            LocalDate date = LocalDate.parse(flightDate);

            // 调用搜索服务
            List<FlightSearchService.FlightSearchResult> results =
                    searchService.searchAvailableFlights(airportFrom, airportTo, date, userId);

            // 转换为前端友好的格式
            List<Map<String, Object>> flightList = convertToFlightList(results);

            // 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", flightList);
            response.put("count", results.size());
            response.put("searchType", "route");
            response.put("message", "搜索 " + airportFrom + " → " + airportTo + " 在 " + flightDate + " 找到 " + results.size() + " 个航班");

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            SimpleHttpServer.sendJsonResponse(exchange, 400,
                    SimpleHttpServer.createErrorResponse("日期格式错误或搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 将搜索结果转换为前端友好的格式
     */
    private List<Map<String, Object>> convertToFlightList(List<FlightSearchService.FlightSearchResult> results) {
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

        return flightList;
    }
}