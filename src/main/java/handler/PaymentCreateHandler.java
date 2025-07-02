package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.alipay.api.AlipayApiException;
import dao.OrderDao;
import dao.PayrecordDao;
import model.Order;
import model.Payrecord;
import service.AlipayService;
import server.SimpleHttpServer;
import utils.JsonUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

public class PaymentCreateHandler implements HttpHandler {
    private OrderDao orderDao = new OrderDao();
    private PayrecordDao payrecordDao = new PayrecordDao();
    private AlipayService alipayService;

    public PaymentCreateHandler() {
        this.alipayService = new AlipayService(null, payrecordDao); // 使用 mock 或配置好的 AlipayClient
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

            if (!"未支付".equals(order.getOrderState())) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("订单状态不允许支付: " + order.getOrderState()));
                return;
            }

            if (payrecordDao.hasPaymentRecord(orderId)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("该订单已有支付记录"));
                return;
            }

            double amount = requestData.containsKey("amount") ? Double.parseDouble((String) requestData.get("amount")) : 0.01;
            Map<String, Object> result = alipayService.createPayment(orderId, amount);
            if ((boolean) result.get("success")) {
                String qrCodeUrl = (String) result.get("qrCode");
                String payId = (String) result.get("payId");
                String htmlResponse = """
                    <html>
                    <body>
                        <h2>请使用支付宝扫描二维码支付</h2>
                        <p>订单号: %s</p>
                        <p>金额: %.2f 元</p>
                        <img src="/qrcode/%s.png" alt="支付宝二维码" style="width: 250px; height: 250px;">
                        <p><a href="%s" target="_blank">直接跳转到支付宝支付</a></p>
                        <script>
                            function checkStatus() {
                                fetch('/api/payments/status?payId=%s')
                                    .then(response => response.json())
                                    .then(data => {
                                        if (data.status === 'TRADE_SUCCESS') {
                                            window.location.href = '/success.html';
                                        } else if (data.status === 'TRADE_CLOSED') {
                                            alert('支付已关闭');
                                        }
                                    }).catch(error => console.error('状态检查失败:', error));
                            }
                            setInterval(checkStatus, 5000); // 每 5 秒检查一次
                        </script>
                    </body>
                    </html>
                """.formatted(orderId, amount, payId, qrCodeUrl, payId);

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, htmlResponse.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlResponse.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                SimpleHttpServer.sendJsonResponse(exchange, 500, result);
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