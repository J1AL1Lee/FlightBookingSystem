package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.BookingService;
import service.PaymentService; // 🆕 添加 PaymentService
import dao.PayrecordDao;
import model.Payrecord;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BookingHandler implements HttpHandler {
    private BookingService bookingService = new BookingService();
    private PaymentService paymentService = new PaymentService(); // 🆕 添加支付服务
    private PayrecordDao payrecordDao = new PayrecordDao(); // 保留用于查询功能

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
            } else if ("POST".equals(method) && path.endsWith("/refund")) {
                handleRefundOrder(exchange);
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
     * 处理创建订单请求 - 通过 PaymentService 处理支付逻辑
     */
    private void handleCreateBooking(HttpExchange exchange) throws IOException {
        System.out.println("🎫 处理创建订单请求");

        try {
            // 解析请求参数 (保持原有的参数解析逻辑)
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📝 请求参数: " + requestBody);

            Map<String, Object> params = JsonUtil.fromJsonToMap(requestBody);
            System.out.println("🔍 解析后的参数Map: " + params);

            String flightrecordId = (String) params.get("flightrecordId");
            String userId = (String) params.get("userId");
            Object seatTypeObj = params.get("seatType");

            // 参数验证逻辑 (保持原有逻辑)
            if (flightrecordId == null || userId == null || seatTypeObj == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：flightrecordId, userId, seatType"));
                return;
            }

            // seatType 转换逻辑 (保持原有逻辑)
            Integer seatType = null;
            try {
                if (seatTypeObj instanceof Integer) {
                    seatType = (Integer) seatTypeObj;
                } else if (seatTypeObj instanceof Double) {
                    seatType = ((Double) seatTypeObj).intValue();
                } else if (seatTypeObj instanceof String) {
                    seatType = Integer.parseInt((String) seatTypeObj);
                } else if (seatTypeObj instanceof Number) {
                    seatType = ((Number) seatTypeObj).intValue();
                }
            } catch (NumberFormatException e) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型格式错误，应为数字0或1"));
                return;
            }

            if (seatType == null || (seatType != 0 && seatType != 1)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型错误，应为0（经济舱）或1（商务舱）"));
                return;
            }

            System.out.println("✅ 参数验证通过: 航班记录=" + flightrecordId + ", 用户=" + userId + ", 座位类型=" + seatType);

            // 🔄 新的三步流程开始 - 通过服务层解耦
            String orderId = null;
            String paymentId = null;

            try {
                // 第一步：创建"未支付"状态的订单
                System.out.println("📝 第一步：创建未支付订单...");
                orderId = bookingService.createUnpaidBooking(flightrecordId, userId, seatType);

                if (orderId == null) {
                    SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("订单创建失败，可能座位不足或其他原因"));
                    return;
                }
                System.out.println("✅ 订单创建成功: " + orderId + " (状态: 未支付)");

                // 第二步：通过 PaymentService 创建支付记录
                System.out.println("💳 第二步：创建支付记录...");
                Integer price = bookingService.calculateOrderPrice(flightrecordId, seatType, userId);

                // 🎯 关键修改：通过 PaymentService 创建支付记录
                paymentId = paymentService.createPayment(orderId, price, "在线支付");

                if (paymentId == null) {
                    System.err.println("❌ 支付记录创建失败，回滚订单");
                    bookingService.cancelOrder(orderId, userId);
                    SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付记录创建失败"));
                    return;
                }

                // 🎯 立即处理支付成功（模拟支付完成）
                boolean paymentProcessed = paymentService.processPaymentSuccess(paymentId);
                if (!paymentProcessed) {
                    System.err.println("❌ 支付处理失败，回滚订单");
                    bookingService.cancelOrder(orderId, userId);
                    SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付处理失败"));
                    return;
                }

                System.out.println("✅ 支付记录创建并处理成功: " + paymentId + " (状态: 已支付)");

                // 第三步：将订单状态更新为"正常"
                System.out.println("🔄 第三步：更新订单状态为正常...");
                boolean statusUpdated = bookingService.updateOrderStatus(orderId, "正常");

                if (!statusUpdated) {
                    System.err.println("❌ 订单状态更新失败，回滚操作");
                    // 通过 PaymentService 取消支付
                    paymentService.cancelPayment(paymentId);
                    // 回滚订单
                    bookingService.cancelOrder(orderId, userId);
                    SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("订单状态更新失败"));
                    return;
                }
                System.out.println("✅ 订单状态更新成功: " + orderId + " -> 正常");

                // 构建成功响应
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("orderId", orderId);
                response.put("paymentId", paymentId);
                response.put("message", "预订成功！订单已确认");
                response.put("orderState", "正常");
                response.put("totalPrice", price);
                response.put("seatType", seatType == 0 ? "经济舱" : "商务舱");

                System.out.println("🎉 订单创建流程完成: 订单=" + orderId + ", 支付=" + paymentId);
                SimpleHttpServer.sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("❌ 订单创建流程异常，进行回滚: " + e.getMessage());

                // 回滚操作 - 通过服务层
                if (paymentId != null) {
                    try {
                        paymentService.cancelPayment(paymentId);
                        System.out.println("🔄 支付记录回滚成功: " + paymentId);
                    } catch (Exception rollbackE) {
                        System.err.println("❌ 支付记录回滚失败: " + rollbackE.getMessage());
                    }
                }

                if (orderId != null) {
                    try {
                        bookingService.cancelOrder(orderId, userId);
                        System.out.println("🔄 订单回滚成功: " + orderId);
                    } catch (Exception rollbackE) {
                        System.err.println("❌ 订单回滚失败: " + rollbackE.getMessage());
                    }
                }

                throw e; // 重新抛出异常
            }

        } catch (Exception e) {
            System.err.println("❌ 创建订单异常: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("创建订单失败: " + e.getMessage()));
        }
    }

    /**
     * 处理取消订单请求 - 通过 PaymentService 处理支付逻辑
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

            // 🎯 通过 PaymentService 取消支付记录
            try {
                Payrecord payrecord = paymentService.getPaymentByOrderId(orderId);
                if (payrecord != null) {
                    paymentService.cancelPayment(payrecord.getPayId());
                    System.out.println("💳 支付记录已取消: " + payrecord.getPayId());
                }
            } catch (Exception e) {
                System.err.println("⚠️ 取消支付记录时出错: " + e.getMessage());
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
     * 🆕 处理退款请求 - 通过 PaymentService 处理
     */
    private void handleRefundOrder(HttpExchange exchange) throws IOException {
        System.out.println("💰 处理订单退款请求");

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            Map<String, Object> params = JsonUtil.fromJsonToMap(requestBody);

            String orderId = (String) params.get("orderId");
            String userId = (String) params.get("userId");
            String reason = (String) params.get("reason");

            if (orderId == null || userId == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：orderId, userId"));
                return;
            }

            if (reason == null || reason.trim().isEmpty()) {
                reason = "用户申请退款";
            }

            // 🎯 通过 PaymentService 处理退款
            String refundId = paymentService.requestRefund(orderId, reason);

            Map<String, Object> response = new HashMap<>();
            if (refundId != null) {
                response.put("success", true);
                response.put("refundId", refundId);
                response.put("message", "退款申请成功");
            } else {
                response.put("success", false);
                response.put("message", "退款申请失败");
            }

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 退款处理异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("退款处理失败: " + e.getMessage()));
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

            // 🎯 通过 PaymentService 获取支付记录
            var paymentRecords = paymentService.getUserPayments(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("paymentRecords", paymentRecords);
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

    // 🗑️ 删除重复的ID生成方法 - 现在由 PaymentService 负责
    // 移除了 generateUniquePayId(), generatePayId(), generateUniqueRefundId(), generateRefundId() 等方法
    // 移除了 createPaymentRecord(), processRefund() 等方法
}