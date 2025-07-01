package handler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.OrderDao;
import dao.PayrecordDao;
import model.Order;
import model.Payrecord;
import utils.JsonUtil;
import server.SimpleHttpServer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SimplePaymentCreateHandler implements HttpHandler {
    private OrderDao orderDao = new OrderDao();
    private PayrecordDao payrecordDao = new PayrecordDao();

    public SimplePaymentCreateHandler() {

    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equals(method) && path.endsWith("/create")) {
                handleCreatePayment(exchange);
            } else if ("POST".equals(method) && path.endsWith("/callback")) {
                handlePaymentCallback(exchange);
            } else if ("POST".equals(method) && path.endsWith("/query")) {
                handleQueryPayment(exchange);
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("接口不存在"));
            }
        } catch (Exception e) {
            System.err.println("❌ SimplePaymentCreateHandler处理失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 创建支付记录
     */
    private void handleCreatePayment(HttpExchange exchange) throws IOException {
        System.out.println("💳 处理支付创建请求");

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            System.out.println("📨 收到支付请求: " + requestBody);

            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);

            String orderId = (String) requestData.get("orderId");
            String payMethod = (String) requestData.get("payMethod");
            Object amountObj = requestData.get("amount");

            // 参数验证
            if (orderId == null || orderId.trim().isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("orderId 不能为空"));
                return;
            }

            if (payMethod == null || payMethod.trim().isEmpty()) {
                payMethod = "在线支付"; // 默认支付方式
            }

            // 查询订单
            Order order = orderDao.findById(orderId);
            if (order == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("订单不存在"));
                return;
            }

            // 检查订单状态
            if (!"未支付".equals(order.getOrderState())) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("订单状态不允许支付: " + order.getOrderState()));
                return;
            }

            // 检查是否已有支付记录
            if (payrecordDao.hasPaymentRecord(orderId)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("该订单已有支付记录"));
                return;
            }

            // 获取支付金额
            Integer amount = null;
            if (amountObj instanceof Integer) {
                amount = (Integer) amountObj;
            } else if (amountObj instanceof String) {
                try {
                    amount = Integer.parseInt((String) amountObj);
                } catch (NumberFormatException e) {
                    SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("支付金额格式错误"));
                    return;
                }
            }

            if (amount == null || amount <= 0) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("支付金额无效"));
                return;
            }

            // 创建支付记录
            String payId = createPaymentRecord(orderId, amount, payMethod);

            if (payId != null) {
                // 更新订单状态为处理中
                orderDao.updateOrderState(orderId, "处理中");

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "支付创建成功");
                responseData.put("payId", payId);
                responseData.put("orderId", orderId);
                responseData.put("amount", amount);
                responseData.put("payMethod", payMethod);
                responseData.put("payState", "处理中");

                SimpleHttpServer.sendJsonResponse(exchange, 200, responseData);
                System.out.println("✅ 支付创建成功: " + payId);
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付记录创建失败"));
            }

        } catch (Exception e) {
            System.err.println("❌ 支付创建失败: " + e.getMessage());
            e.printStackTrace();
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付创建失败: " + e.getMessage()));
        }
    }

    /**
     * 处理支付回调（模拟支付成功/失败）
     */
    private void handlePaymentCallback(HttpExchange exchange) throws IOException {
        System.out.println("📞 处理支付回调");

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);

            String payId = (String) requestData.get("payId");
            String status = (String) requestData.get("status"); // "success" 或 "failed"

            if (payId == null || status == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("缺少必需参数：payId, status"));
                return;
            }

            // 查询支付记录
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("支付记录不存在"));
                return;
            }

            // 处理支付结果
            boolean success = false;
            if ("success".equals(status)) {
                success = processPaymentSuccess(payId, payrecord.getOrderId());
            } else if ("failed".equals(status)) {
                success = processPaymentFailure(payId, payrecord.getOrderId(), "支付失败");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "支付回调处理成功" : "支付回调处理失败");
            response.put("payId", payId);

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 支付回调处理失败: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付回调处理失败: " + e.getMessage()));
        }
    }

    /**
     * 查询支付状态
     */
    private void handleQueryPayment(HttpExchange exchange) throws IOException {
        System.out.println("🔍 处理支付查询请求");

        try {
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);

            String payId = (String) requestData.get("payId");
            if (payId == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("payId 不能为空"));
                return;
            }

            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("支付记录不存在"));
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("payId", payrecord.getPayId());
            response.put("orderId", payrecord.getOrderId());
            response.put("amount", payrecord.getPayment());
            response.put("payMethod", payrecord.getPayMethod());
            response.put("payState", payrecord.getPayState());
            response.put("payTime", payrecord.getPayTime().toString());

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("❌ 支付查询失败: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付查询失败: " + e.getMessage()));
        }
    }

    /**
     * 创建支付记录
     */
    private String createPaymentRecord(String orderId, Integer amount, String payMethod) {
        try {
            // 生成支付ID
            String payId = generateUniquePayId();

            // 创建支付记录对象
            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(payId);
            payrecord.setOrderId(orderId);
            payrecord.setPayment(amount);
            payrecord.setPayMethod(payMethod);
            payrecord.setPayState("处理中");
            payrecord.setPayTime(LocalDateTime.now());

            // 保存到数据库
            String savedPayId = payrecordDao.save(payrecord);
            System.out.println("💳 支付记录创建成功: " + savedPayId);
            return savedPayId;

        } catch (Exception e) {
            System.err.println("❌ 创建支付记录失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 处理支付成功
     */
    private boolean processPaymentSuccess(String payId, String orderId) {
        try {
            // 更新支付状态
            payrecordDao.updateStatus(payId, "已支付");

            // 更新订单状态
            boolean orderUpdated = orderDao.updateOrderState(orderId, "已支付");
            if (!orderUpdated) {
                System.err.println("❌ 更新订单状态失败");
                return false;
            }

            System.out.println("✅ 支付成功处理完成: 支付ID=" + payId + ", 订单ID=" + orderId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理支付成功异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理支付失败
     */
    private boolean processPaymentFailure(String payId, String orderId, String reason) {
        try {
            // 更新支付状态
            payrecordDao.updateStatus(payId, "支付失败");

            // 更新订单状态
            boolean orderUpdated = orderDao.updateOrderState(orderId, "支付失败");
            if (!orderUpdated) {
                System.err.println("❌ 更新订单状态失败");
                return false;
            }

            System.out.println("❌ 支付失败处理完成: 支付ID=" + payId + ", 订单ID=" + orderId + ", 原因=" + reason);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理支付失败异常: " + e.getMessage());
            return false;
        }
    }

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

    /**
     * 生成支付ID
     */
    private String generatePayId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        String randomNum = String.format("%06d", random.nextInt(1000000));
        return "PAY" + date + randomNum;
    }
}