package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.google.gson.Gson;
import dao.PayrecordDao;
import model.Payrecord;
import server.SimpleHttpServer;
import java.io.IOException;
import java.util.Map;

public class PaymentStatusHandler implements HttpHandler {
    private PayrecordDao payrecordDao = new PayrecordDao();
    private AlipayClient alipayClient;

    public PaymentStatusHandler(AlipayClient alipayClient) {
        this.alipayClient = alipayClient; // 注入 AlipayClient
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            SimpleHttpServer.sendJsonResponse(exchange, 405, SimpleHttpServer.createErrorResponse("只支持GET请求"));
            return;
        }

        try {
            String query = exchange.getRequestURI().getQuery();
            String[] params = query.split("&");
            String payId = "";
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv[0].equals("payId")) payId = kv[1];
            }
            if (payId.isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("payId 不能为空"));
                return;
            }

            // 调用 AlipayService 的 checkPaymentStatus
            Map<String, Object> result = new service.AlipayService(alipayClient, payrecordDao).checkPaymentStatus(payId);
            SimpleHttpServer.sendJsonResponse(exchange, 200, result);
        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝查询异常: " + e.getMessage());
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("支付宝查询失败: " + e.getMessage()));
        }
    }
}