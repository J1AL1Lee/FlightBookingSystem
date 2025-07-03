package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.OrderService;
import dto.OrderDetailDTO;
import dto.OrderStatsDTO;
import server.SimpleHttpServer;
import utils.JsonUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单管理Handler - 处理订单相关的HTTP请求
 * 基于现有的BookingService架构，提供订单管理功能
 */
public class OrderManagementHandler implements HttpHandler {

    private OrderService orderService;

    public OrderManagementHandler() {
        this.orderService = new OrderService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置CORS头
        SimpleHttpServer.setCorsHeaders(exchange);

        // 处理OPTIONS预检请求
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            System.out.println("🔍 处理订单请求: " + method + " " + path);

            // 验证用户登录状态
            Map<String, Object> currentUser = SimpleHttpServer.getCurrentUser(exchange);
            if (currentUser == null) {
                SimpleHttpServer.sendUnauthorizedResponse(exchange);
                return;
            }

            String userId = (String) currentUser.get("userId");
            System.out.println("👤 当前用户: " + userId);

            // 根据路径分发请求
            if (path.endsWith("/orders/user") && "GET".equals(method)) {
                handleGetUserOrders(exchange, userId);
            } else if (path.endsWith("/orders/stats") && "GET".equals(method)) {
                handleGetOrderStats(exchange, userId);
            } else if (path.endsWith("/orders/cancel") && "POST".equals(method)) {
                handleCancelOrder(exchange, userId);
            } else if (path.endsWith("/orders/refund") && "POST".equals(method)) {
                handleRequestRefund(exchange, userId);
            } else {
                sendErrorResponse(exchange, 404, "接口不存在");
            }

        } catch (Exception e) {
            System.err.println("❌ 订单请求处理异常: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 获取用户订单列表
     */
    private void handleGetUserOrders(HttpExchange exchange, String userId) throws IOException {
        try {
            // 获取查询参数
            String query = exchange.getRequestURI().getQuery();
            String orderState = "all"; // 默认查询所有订单

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2 && "state".equals(kv[0])) {
                        orderState = kv[1];
                    }
                }
            }

            System.out.println("🔍 查询订单状态: " + orderState);

            // 查询订单详情
            List<OrderDetailDTO> orders;
            if ("all".equals(orderState)) {
                orders = orderService.getUserOrderDetails(userId);
            } else {
                orders = orderService.getUserOrdersByState(userId, orderState);
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", orders);
            response.put("count", orders.size());

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);
            System.out.println("✅ 订单查询成功，返回 " + orders.size() + " 条记录");

        } catch (Exception e) {
            System.err.println("❌ 查询订单失败: " + e.getMessage());
            sendErrorResponse(exchange, 500, "查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单统计信息
     */
    private void handleGetOrderStats(HttpExchange exchange, String userId) throws IOException {
        try {
            OrderStatsDTO stats = orderService.getUserOrderStats(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "统计查询成功");
            response.put("data", stats);

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);
            System.out.println("✅ 订单统计查询成功");

        } catch (Exception e) {
            System.err.println("❌ 查询订单统计失败: " + e.getMessage());
            sendErrorResponse(exchange, 500, "查询订单统计失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    private void handleCancelOrder(HttpExchange exchange, String userId) throws IOException {
        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📥 取消订单请求: " + requestBody);

            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
            String orderId = (String) requestData.get("orderId");

            if (orderId == null || orderId.trim().isEmpty()) {
                sendErrorResponse(exchange, 400, "订单号不能为空");
                return;
            }

            boolean success = orderService.cancelOrder(orderId, userId);

            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "订单取消成功");
                SimpleHttpServer.sendJsonResponse(exchange, 200, response);
                System.out.println("✅ 订单取消成功: " + orderId);
            } else {
                response.put("success", false);
                response.put("message", "订单取消失败");
                SimpleHttpServer.sendJsonResponse(exchange, 400, response);
                System.out.println("❌ 订单取消失败: " + orderId);
            }

        } catch (Exception e) {
            System.err.println("❌ 取消订单异常: " + e.getMessage());
            sendErrorResponse(exchange, 500, "取消订单失败: " + e.getMessage());
        }
    }

    /**
     * 申请退款
     */
    private void handleRequestRefund(HttpExchange exchange, String userId) throws IOException {
        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📥 退款申请请求: " + requestBody);

            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
            String orderId = (String) requestData.get("orderId");
            String reason = (String) requestData.get("reason");

            if (orderId == null || orderId.trim().isEmpty()) {
                sendErrorResponse(exchange, 400, "订单号不能为空");
                return;
            }

            if (reason == null || reason.trim().isEmpty()) {
                reason = "用户申请退款"; // 默认退款原因
            }

            // 直接处理退款（更新订单状态和支付记录）
            boolean success = orderService.processRefund(orderId, userId, reason);

            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "退款成功，订单已取消");
                SimpleHttpServer.sendJsonResponse(exchange, 200, response);
                System.out.println("✅ 退款处理成功: " + orderId);
            } else {
                response.put("success", false);
                response.put("message", "退款失败，请检查订单状态");
                SimpleHttpServer.sendJsonResponse(exchange, 400, response);
                System.out.println("❌ 退款处理失败: " + orderId);
            }

        } catch (Exception e) {
            System.err.println("❌ 申请退款异常: " + e.getMessage());
            sendErrorResponse(exchange, 500, "申请退款失败: " + e.getMessage());
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        SimpleHttpServer.sendJsonResponse(exchange, code, response);
    }
}