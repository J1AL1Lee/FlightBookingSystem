package handler;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.google.gson.Gson;
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
import java.util.HashMap;
import java.util.Map;

public class PaymentCreateHandler implements HttpHandler {
    private OrderDao orderDao = new OrderDao();
    private AlipayClient alipayClient;

    public PaymentCreateHandler() {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        config.setAppId("9021000149697288"); // 替换为你的沙箱 AppID
        config.setPrivateKey("your_private_key"); // 替换为你的私钥
        config.setAlipayPublicKey("your_alipay_public_key"); // 替换为支付宝公钥
        config.setCharset("UTF-8");
        config.setSignType("RSA2");
        try {
            this.alipayClient = new DefaultAlipayClient(config);
        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝客户端初始化失败: " + e.getMessage());
            throw new RuntimeException("支付宝客户端初始化失败", e); // 转换为 RuntimeException，适配现有结构
        }
    }

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
            System.out.println("📨 收到支付请求: " + requestBody);

            Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
            String orderId = (String) requestData.get("orderId");
            if (orderId == null || orderId.trim().isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("orderId 不能为空"));
                return;
            }

            Order order = orderDao.findById(orderId);
            if (order == null) {
                SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("订单不存在"));
                return;
            }

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderId);
            bizContent.put("total_amount", "0.01"); // 沙箱测试金额
            bizContent.put("subject", "Flight Booking Payment for Order " + orderId);
            request.setBizContent(new Gson().toJson(bizContent));

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                Payrecord payrecord = new Payrecord();
                payrecord.setPayId(response.getOutTradeNo());
                payrecord.setOrderId(orderId);
                payrecord.setPayment(1); // 沙箱测试金额 0.01 元
                payrecord.setPayMethod("Alipay");
                payrecord.setPayState("等待支付");
                payrecord.setPayTime(LocalDateTime.now());
                orderDao.save(order); // 假设更新订单状态
                new PayrecordDao().save(payrecord);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "支付创建成功");
                responseData.put("payId", response.getOutTradeNo());
                responseData.put("qrCode", response.getQrCode());
                SimpleHttpServer.sendJsonResponse(exchange, 200, responseData);
                System.out.println("✅ 支付创建成功: " + orderId);
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付创建失败: " + response.getMsg()));
            }
        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝 API 异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付宝调用失败: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ 支付处理失败: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付处理失败: " + e.getMessage()));
        }
    }
}
