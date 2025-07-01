package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.BookingService;
import dao.PayrecordDao;
import model.Payrecord;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BookingHandler implements HttpHandler {
    private BookingService bookingService = new BookingService();
    private PayrecordDao payrecordDao = new PayrecordDao(); // 🆕 添加支付记录DAO

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
            } else if ("POST".equals(method) && path.endsWith("/refund")) { // 🆕 添加退款接口
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
     * 处理创建订单请求 - 直接生成"正常"状态订单并创建支付记录
     */
    // 在 handleCreateBooking 方法中添加详细的调试信息

    private void handleCreateBooking(HttpExchange exchange) throws IOException {
        System.out.println("🎫 处理创建订单请求");

        try {
            // 解析请求参数
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📝 请求参数: " + requestBody);

            Map<String, Object> params = JsonUtil.fromJsonToMap(requestBody);
            System.out.println("🔍 解析后的参数Map: " + params);

            String flightrecordId = (String) params.get("flightrecordId");
            String userId = (String) params.get("userId");
            Object seatTypeObj = params.get("seatType");

            // 🔍 详细调试 seatType
            System.out.println("🔍 seatTypeObj 原始值: " + seatTypeObj);
            System.out.println("🔍 seatTypeObj 类型: " + (seatTypeObj != null ? seatTypeObj.getClass().getName() : "null"));

            // 参数验证
            if (flightrecordId == null || userId == null || seatTypeObj == null) {
                System.err.println("❌ 参数缺失检查:");
                System.err.println("   flightrecordId: " + flightrecordId);
                System.err.println("   userId: " + userId);
                System.err.println("   seatTypeObj: " + seatTypeObj);
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：flightrecordId, userId, seatType"));
                return;
            }

            // 🔧 更详细的 seatType 处理
            Integer seatType = null;
            try {
                if (seatTypeObj instanceof Integer) {
                    seatType = (Integer) seatTypeObj;
                    System.out.println("✅ seatType 是 Integer: " + seatType);
                } else if (seatTypeObj instanceof Double) {
                    seatType = ((Double) seatTypeObj).intValue();
                    System.out.println("✅ seatType 从 Double 转换: " + seatType);
                } else if (seatTypeObj instanceof String) {
                    seatType = Integer.parseInt((String) seatTypeObj);
                    System.out.println("✅ seatType 从 String 转换: " + seatType);
                } else if (seatTypeObj instanceof Number) {
                    seatType = ((Number) seatTypeObj).intValue();
                    System.out.println("✅ seatType 从 Number 转换: " + seatType);
                } else {
                    System.err.println("❌ 无法识别的 seatType 类型: " + seatTypeObj.getClass().getName());
                }
            } catch (NumberFormatException e) {
                System.err.println("❌ seatType 格式转换失败: " + seatTypeObj + " (类型: " + seatTypeObj.getClass().getName() + ")");
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型格式错误，应为数字0或1"));
                return;
            }

            System.out.println("🔍 最终的 seatType: " + seatType);

            // 验证 seatType 值的有效性
            if (seatType == null) {
                System.err.println("❌ seatType 转换后为 null");
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型转换失败"));
                return;
            }

            if (seatType != 0 && seatType != 1) {
                System.err.println("❌ seatType 值无效: " + seatType + "，应为0或1");
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("座位类型错误，应为0（经济舱）或1（商务舱）"));
                return;
            }

            System.out.println("✅ 参数验证通过: 航班记录=" + flightrecordId + ", 用户=" + userId + ", 座位类型=" + seatType);

            // 继续原有的业务逻辑...
            String orderId = bookingService.createBooking(flightrecordId, userId, seatType);

            if (orderId != null) {
                // 计算价格
                Integer price = bookingService.calculateOrderPrice(flightrecordId, seatType, userId);

                // 🆕 创建支付记录
                String paymentId = createPaymentRecord(orderId, price, "系统支付");

                // 订单创建成功，直接更新为"正常"状态（跳过支付）
                boolean confirmed = bookingService.confirmOrder(orderId);

                if (confirmed) {
                    // 构建成功响应
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("orderId", orderId);
                    response.put("paymentId", paymentId); // 🆕 返回支付记录ID
                    response.put("message", "预订成功！订单已确认");
                    response.put("orderState", "正常");
                    response.put("totalPrice", price);
                    response.put("seatType", seatType == 0 ? "经济舱" : "商务舱");

                    System.out.println("✅ 订单创建并确认成功: " + orderId + "，支付记录: " + paymentId);
                    SimpleHttpServer.sendJsonResponse(exchange, 200, response);
                } else {
                    // 确认失败，需要回滚
                    System.err.println("❌ 订单确认失败，尝试取消订单");
                    bookingService.cancelOrder(orderId, userId);

                    // 🆕 同时删除支付记录
                    if (paymentId != null) {
                        payrecordDao.deletePayrecord(paymentId);
                        System.out.println("🔄 支付记录已回滚: " + paymentId);
                    }

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
     * 🆕 创建支付记录
     */
    private String createPaymentRecord(String orderId, Integer amount, String payMethod) {
        try {
            if (amount == null || amount <= 0) {
                System.err.println("❌ 支付金额无效: " + amount);
                return null;
            }

            // 生成支付ID
            String payId = generateUniquePayId();

            // 创建支付记录
            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(payId);
            payrecord.setOrderId(orderId);
            payrecord.setPayment(amount);
            payrecord.setPayMethod(payMethod);
            payrecord.setPayState("已支付"); // 直接设为已支付，匹配订单状态
            payrecord.setPayTime(LocalDateTime.now());

            // 保存支付记录
            String savedPayId = payrecordDao.save(payrecord);
            System.out.println("💳 支付记录创建成功: " + savedPayId);
            return savedPayId;

        } catch (Exception e) {
            System.err.println("❌ 创建支付记录失败: " + e.getMessage());
            return null;
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

            // 🆕 先取消支付记录
            try {
                Payrecord payrecord = payrecordDao.findByOrderId(orderId);
                if (payrecord != null) {
                    payrecordDao.updateStatus(payrecord.getPayId(), "已取消");
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
     * 🆕 处理退款请求
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

            // 处理退款
            String refundId = processRefund(orderId, userId, reason);

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
     * 🆕 处理退款逻辑
     */
    private String processRefund(String orderId, String userId, String reason) {
        try {
            // 验证订单
            var order = bookingService.getOrderById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                System.err.println("❌ 订单验证失败");
                return null;
            }

            if (!"已支付".equals(order.getOrderState()) && !"正常".equals(order.getOrderState())) {
                System.err.println("❌ 订单状态不允许退款: " + order.getOrderState());
                return null;
            }

            // 获取原支付记录
            Payrecord originalPayment = payrecordDao.findByOrderId(orderId);
            if (originalPayment == null) {
                System.err.println("❌ 找不到订单的支付记录");
                return null;
            }

            // 创建退款记录
            String refundId = generateUniqueRefundId();
            Payrecord refundPayment = new Payrecord();
            refundPayment.setPayId(refundId);
            refundPayment.setOrderId(orderId);
            refundPayment.setPayment(-originalPayment.getPayment()); // 负数表示退款
            refundPayment.setPayMethod("退款");
            refundPayment.setPayState("退款成功");
            refundPayment.setPayTime(LocalDateTime.now());

            // 保存退款记录
            String savedRefundId = payrecordDao.save(refundPayment);
            if (savedRefundId != null) {
                // 更新原支付记录状态
                payrecordDao.updateStatus(originalPayment.getPayId(), "已退款");

                // 更新订单状态为已取消
                bookingService.cancelOrder(orderId, userId);

                System.out.println("✅ 退款处理成功: " + savedRefundId);
                return savedRefundId;
            }

            return null;

        } catch (Exception e) {
            System.err.println("❌ 退款处理失败: " + e.getMessage());
            return null;
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

            // 🆕 同时获取支付记录
            var paymentRecords = payrecordDao.findByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("paymentRecords", paymentRecords); // 🆕 返回支付记录
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

    // 🆕 ID生成方法

    /**
     * 生成唯一支付ID
     */
    private String generateUniquePayId() {
        String payId;
        int attempts = 0;
        do {
            payId = generatePayId();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("生成唯一支付ID失败，尝试次数过多");
            }
        } while (payrecordDao.isPayIdExists(payId));

        return payId;
    }

    private String generatePayId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        String randomNum = String.format("%06d", random.nextInt(1000000));
        return "PAY" + date + randomNum;
    }

    /**
     * 生成唯一退款ID
     */
    private String generateUniqueRefundId() {
        String refundId;
        int attempts = 0;
        do {
            refundId = generateRefundId();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("生成唯一退款ID失败，尝试次数过多");
            }
        } while (payrecordDao.isPayIdExists(refundId));

        return refundId;
    }

    private String generateRefundId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        String randomNum = String.format("%06d", random.nextInt(1000000));
        return "REF" + date + randomNum;
    }
}