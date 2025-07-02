package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import dao.OrderDao;
import dao.PayrecordDao;
import model.Order;
import model.Payrecord;
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
    private AlipayClient alipayClient;

    public PaymentCreateHandler() {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        config.setAppId("9021000149697288"); // 替换为你的沙箱 AppID
        config.setPrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8j80DL2o11vTtip6bhPZNE+dmhQarBCSnYzD3TYLI4WYRA5FyYfw1LolsYjgOK+v4OeiMk31NLfrlXBZCpFEiEnMDU6bVlIMIN6uD/MtvrLyf0uepGLso01B8UrB7jja/F81kfutkeQVdxPsl67XRZ2qPax6HGBA8msaoggoEr2k/xd9tBByYwYbaF5UueSfpjj6FheOr4cFzFOy1POWPAYqRM6XxQK+w7i5xl+ah7IdmJsOyGRImdg4J3LHlQJ8XkjfsmzgqNq93WGVrXH0Egrgrx5VZm4IgVP6fZfSjhhxokkj/dhPk/idWYrURzSfiJ0PUvRMaL8Gzx7TuBKhHAgMBAAECggEBAKAIF4HdivG4xtSXsjbhaLxP6TNcMSWRdZ5Ok+8/bIEasyo7cgS23nswTNecoGB+rF1WoGQ2hMCtBmQEfKwAkw8sw0oOg+h+i5q8zKdPNEVKQCgQsiYUZDuo5IUvFLM4JoSWKe5hvVvfTkuf81riqsPXVlv0GMulA5q77WB0RRZlZ3+01Vhkz4PIeWce23endgd7RjGfNhpq7Z/OKtl7N+jChjGToUTL6s3TsEirbP9DcQYO1M8ETLFkYTXViMt3Z3lB3fMEcjX53cc9DqdbuRX0+K4HnPtBIlRKOvpf6+l8Vb6s6d8OTSWHuj1/1cegpE5M7d+bZ/VjPEFVF2FdayECgYEA6TFeR/rtSvmdowed6OdUmA1hgPJidaGuP3jsVY/hyNnSGhkbE7WssNmC7HXeVVjRC81iULaSr9kJc2Rh0O5EGrn0GarKFkA3kyBE50YVqSkIrCIpFtydFdNKvpAepQDu0BfoxJsDac2BfPrvCbS35J3HZ/sHrSNXqWlQV0Cm2fECgYEAzwD32/HdfVKby6IEpSVqW14v3yyPElShHQL7j3TluLneZbi9cN3eZxA9ebNRhe60wtoAGWOx0LS12fHWg2FsuTywcdhVTbzkElog33lzlLwEko6fgbE4ANL9q48vRmxptWDKivEl3pW9qgP+SLD/+xJBgjLto6KX0SHlxh3nrbcCgYBqFlqVFpQTsuHDRHjTd0Jl9lhwaFTgvRBfseyatF18mZPa6acG3XTV8+57Eth2LXTVELf0jkrHk06YX4ecnHkBS63Aa5GKc+aUmW6fZKQAFDnszZGx4+XXAwwTC8/VM0pyAx6TKw5veN269RIAcWXjrOAF7w879kMwQEgbmb8OkQKBgQCAY9FXkcQWns4SlwLai0JUOS7n9PMoI2VqYRc1+wMgd+gAn3ygLHxs4B3BBf9iWpOy5xN4q+T11Z+U9fJeumZ83a9ybQM7nBS5bT1GXkXZ0mPjoqI8Bnb9y9+aMMzZmRRXcxks5DTgwW9JrABjhaS/TKtk3cGW5JnVFHk3UAUKMQKBgE3PJY5gVB+Gpg+hElNV+IWrIZ6atDE5HEtMJrYjnuRSchuwkW2pIGwsOAya83LLvk6Of6V5oSM0Mn9LcKpgO++AiS85zIS61CHPhetirFDDqhqvzK6Xpp9rWrIUnOU4wHToQP4VWRiSPYkU/PpXcDEeOG7C4/IghkzpJfzNMqHP"); // 替换为你的私钥
        config.setAlipayPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmlEmO339xgGfOex/PGyi7HyhD7XTYb3QpUmlK2Z2Qnk0eSQ2dtOOo19tdZgBnFM7n9ybBC4y/uchhvcV59tDx8c4h82ST8H7wf2TfldsfXIbk10/z5xuSeqnoQ4aXLx8UfXMywMLT+Ytpz2+75rTkWeE4/q1lCDBRZ+0qHcUmCTNy4Mg554lWfQ60XmpJawvOB0jjM2zlFggoG4V0ieJCdcI+3fnw1WP5/probDo1ZPSr3b9zu1Y3XoDO0smADp4+4NHuJEQCL7R06ModhemFUm+FI6V1H7pj1zrt2mR3pXD7CasVY3nEGLyO59oGSGvpPottKQem7IvpujoU+Do/wIDAQAB"); // 替换为支付宝公钥
        config.setCharset("UTF-8");
        config.setSignType("RSA2");
        try {
            this.alipayClient = new DefaultAlipayClient(config);
        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝客户端初始化失败: " + e.getMessage());
            throw new RuntimeException("支付宝客户端初始化失败", e);
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

            if (!"未支付".equals(order.getOrderState())) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("订单状态不允许支付: " + order.getOrderState()));
                return;
            }

            if (payrecordDao.hasPaymentRecord(orderId)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("该订单已有支付记录"));
                return;
            }

            double amount = requestData.containsKey("amount") ? Double.parseDouble((String) requestData.get("amount")) : 0.01;
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderId);
            bizContent.put("total_amount", String.format("%.2f", amount));
            bizContent.put("subject", "Flight Booking Payment for Order " + orderId);
            request.setBizContent(new Gson().toJson(bizContent));

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                Payrecord payrecord = new Payrecord();
                payrecord.setPayId(response.getOutTradeNo());
                payrecord.setOrderId(orderId);
                payrecord.setPayment((int) (amount * 100));
                payrecord.setPayMethod("Alipay");
                payrecord.setPayState("等待支付");
                payrecord.setPayTime(LocalDateTime.now());
                payrecordDao.save(payrecord);

                // 构造包含二维码的 HTML 响应
                String qrCodeUrl = response.getQrCode();
                String htmlResponse = """
                    <html>
                    <body>
                        <h2>请使用支付宝扫描二维码支付</h2>
                        <p>订单号: %s</p>
                        <p>金额: %.2f 元</p>
                        <img src="%s" alt="支付宝二维码" style="width: 250px; height: 250px;">
                        <p><a href="%s" target="_blank">直接跳转到支付宝支付</a></p>
                        <p>支付完成后，请返回查询状态: <a href="/api/payments/status?payId=%s">查询支付状态</a></p>
                        <script>
                            // 自动刷新状态（每5秒一次）
                            setInterval(() => {
                                fetch('/api/payments/status?payId=%s')
                                    .then(response => response.json())
                                    .then(data => {
                                        if (data.status === 'TRADE_SUCCESS') {
                                            alert('支付成功！');
                                            window.location.href = '/personal.html'; // 跳转到个人中心
                                        }
                                    });
                            }, 5000);
                        </script>
                    </body>
                    </html>
                """.formatted(orderId, amount, qrCodeUrl, qrCodeUrl, response.getOutTradeNo(), response.getOutTradeNo());

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, htmlResponse.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlResponse.getBytes(StandardCharsets.UTF_8));
                }
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