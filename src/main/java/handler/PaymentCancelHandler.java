package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import dao.PayrecordDao;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.Map;

public class PaymentCancelHandler implements HttpHandler {
    private AlipayClient alipayClient;
    private PayrecordDao payrecordDao;

    public PaymentCancelHandler(AlipayClient alipayClient, PayrecordDao payrecordDao) {
        this.alipayClient = alipayClient;
        this.payrecordDao = payrecordDao;
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
            String payId = SimpleHttpServer.readRequestBody(exchange).trim();
            if (payId.isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("payId 不能为空"));
                return;
            }

            Map<String, Object> result = new service.AlipayService(alipayClient, payrecordDao).cancelPayment(payId);
            SimpleHttpServer.sendJsonResponse(exchange, (boolean) result.get("success") ? 200 : 500, result);
        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝取消异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付宝取消失败: " + e.getMessage()));
        }
    }
}