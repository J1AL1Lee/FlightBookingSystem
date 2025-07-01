package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.BookingService;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BookingHandler implements HttpHandler {
    private BookingService bookingService = new BookingService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置CORS头
        SimpleHttpServer.setCorsHeaders(exchange);

        // 处理OPTIONS预检请求
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equals(method) && path.endsWith("/create")) {
                handleCreateBooking(exchange);
            } else if ("POST".equals(method) && path.endsWith("/cancel")) {
                handleCancelBooking(exchange);
            } else if ("GET".equals(method) && path.contains("/price")) {
                handleGetPrice(exchange);
            } else if ("GET".equals(method) && path.contains("/orders")) {
                handleGetUserOrders(exchange);
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("接口不存在"));
            }
        } catch (Exception e) {
            System.err.println("❌ BookingHandler处理失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 处理创建订单请求 - 直接生成"正常"状态订单
     */
    private void handleCreateBooking(HttpExchange exchange) throws IOException {
        System.out.println("🎫 处理创建订单请求");

        try {
            // 解析请求参数
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📝 请求参数: " + requestBody);

            Map<String, Object> params = JsonUtil.fromJsonToMap(requestBody);

            String flightrecordId = (String) params.get("flightrecordId");
            String userId = (String) params.get("userId");
            Object seatTypeObj = params.get("seatType");

            // 参数验证
            if (flightrecordId == null || userId == null || seatTypeObj == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：flightrecordId, userId, seatType"));
                return;
            }

            Integer seatType = null;
            if (seatTypeObj instanceof Integer) {
                seatType = (Integer) seatTypeObj;
            } else if (seatTypeObj instanceof String) {
                try {
                    seatType = Integer.parseInt((String) seatTypeObj);
                } catch (NumberFormatException e) {
                    SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型格式错误"));
                    return;
                }
            }

            if (seatType == null || (seatType != 0 && seatType != 1)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型错误，应为0（经济舱）或1（商务舱）"));
                return;
            }

            System.out.println("🔍 解析参数: 航班记录=" + flightrecordId + ", 用户=" + userId + ", 座位类型=" + seatType);

            // 调用业务逻辑创建订单
            String orderId = bookingService.createBooking(flightrecordId, userId, seatType);

            if (orderId != null) {
                // 订单创建成功，直接更新为"正常"状态（跳过支付）
                boolean confirmed = bookingService.confirmOrder(orderId);

                if (confirmed) {
                    // 计算价格用于显示
                    Integer price = bookingService.calculateOrderPrice(flightrecordId, seatType, userId);

                    // 构建成功响应
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("orderId", orderId);
                    response.put("message", "预订成功！订单已确认");
                    response.put("orderState", "正常");
                    response.put("totalPrice", price);
                    response.put("seatType", seatType == 0 ? "经济舱" : "商务舱");

                    System.out.println("✅ 订单创建并确认成功: " + orderId);
                    SimpleHttpServer.sendJsonResponse(exchange, 200, response);
                } else {
                    // 确认失败，需要回滚
                    System.err.println("❌ 订单确认失败，尝试取消订单");
                    bookingService.cancelOrder(orderId, userId);
                    SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("订单确认失败"));
                }
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("订单创建失败，可能座位不足或其他原因"));
            }

        } catch (Exception e) {
            System.err.println("❌ 创建订单异常: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("创建订单失败: " + e.getMessage()));
        }
    }

    /**
     * 处理取消订单请求
     */
    private void handleCancelBooking(HttpExchange exchange) throws IOException {
        System.out.println("🚫 处理取消订单请求");

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            Map<String, Object> params = JsonUtil.fromJsonToMap(requestBody);

            String orderId = (String) params.get("orderId");
            String userId = (String) params.get("userId");

            if (orderId == null || userId == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：orderId, userId"));
                return;
            }

            boolean success = bookingService.cancelOrder(orderId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "订单取消成功" : "订单取消失败");

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 取消订单异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("取消订单失败: " + e.getMessage()));
        }
    }

    /**
     * 处理价格查询请求
     */
    private void handleGetPrice(HttpExchange exchange) throws IOException {
        System.out.println("💰 处理价格查询请求");

        try {
            // 解析URL参数
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQueryParams(query);

            String flightrecordId = queryParams.get("flightrecordId");
            String seatTypeStr = queryParams.get("seatType");
            String userId = queryParams.get("userId");

            if (flightrecordId == null || seatTypeStr == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：flightrecordId, seatType"));
                return;
            }

            Integer seatType = Integer.parseInt(seatTypeStr);
            Integer price = bookingService.calculateOrderPrice(flightrecordId, seatType, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("price", price);
            response.put("seatType", seatType == 0 ? "经济舱" : "商务舱");

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 价格查询异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("价格查询失败: " + e.getMessage()));
        }
    }

    /**
     * 处理用户订单查询请求
     */
    private void handleGetUserOrders(HttpExchange exchange) throws IOException {
        System.out.println("📋 处理用户订单查询请求");

        try {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQueryParams(query);

            String userId = queryParams.get("userId");
            if (userId == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：userId"));
                return;
            }

            var orders = bookingService.getUserOrders(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("count", orders.size());

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 查询订单异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("查询订单失败: " + e.getMessage()));
        }
    }

    /**
     * 解析URL查询参数
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
}
