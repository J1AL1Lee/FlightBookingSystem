package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.DatabaseConnection;
import dao.PayrecordDao;
import dao.OrderDao;
import model.Payrecord;
import service.AlipayService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import handler.PaymentCreateHandler;
import handler.PaymentStatusHandler;
import handler.PaymentCancelHandler;
import handler.RegisterHandler;
import handler.LoginHandler;
import handler.UsersHandler;
import handler.HelloHandler;
import handler.TestHandler;
import handler.SimpleFlightSearchHandler;
import handler.BookingHandler;
import handler.ResourceBasedStaticHandler;
import utils.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class PayServer {

    private HttpServer httpServer;
    private final int port;
    private final AlipayClient alipayClient;

    public PayServer(int port) {
        this.port = port;
        this.alipayClient = initAlipayClient();
    }

    private AlipayClient initAlipayClient() {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        config.setAppId("9021000149697288");
        config.setPrivateKey("your_private_key"); // 替换为实际私钥
        config.setAlipayPublicKey("your_alipay_public_key"); // 替换为实际公钥
        config.setCharset("UTF-8");
        config.setSignType("RSA2");
        try {
            return new DefaultAlipayClient(config);
        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝客户端初始化失败: " + e.getMessage());
            throw new RuntimeException("支付宝客户端初始化失败", e);
        }
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // API路径
        httpServer.createContext("/api/register", new RegisterHandler());
        httpServer.createContext("/api/login", new LoginHandler());
        httpServer.createContext("/api/users", new UsersHandler());
        httpServer.createContext("/hello", new HelloHandler());
        httpServer.createContext("/test", new TestHandler());
        httpServer.createContext("/api/flights/search", new SimpleFlightSearchHandler());

        // 🎫 添加预订相关路由
        httpServer.createContext("/api/booking/create", new BookingHandler());
        httpServer.createContext("/api/booking/cancel", new BookingHandler());
        httpServer.createContext("/api/booking/price", new BookingHandler());
        httpServer.createContext("/api/booking/orders", new BookingHandler());
        httpServer.createContext("/api/booking/refund", new BookingHandler());

        // 主支付相关路由，使用支付宝
        httpServer.createContext("/api/payments/create", new PaymentCreateHandler());
        httpServer.createContext("/api/payments/status", new PaymentStatusHandler(alipayClient));
        httpServer.createContext("/api/payments/cancel", new PaymentCancelHandler(alipayClient, new PayrecordDao()));
        httpServer.createContext("/api/payments/notify", new PaymentNotifyHandler());

        // 使用新的资源处理器
        httpServer.createContext("/", new ResourceBasedStaticHandler());

        // 添加二维码图片服务
        httpServer.createContext("/qrcode", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath().substring(1); // 移除前缀 /qrcode
                File file = new File(path + ".png");
                if (file.exists()) {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "image/png");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("二维码图片未找到"));
                }
            }
        });

        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("HttpServer started on port " + port);
    }

    public void stop(int delay) {
        if (httpServer != null) {
            httpServer.stop(delay);
            System.out.println("HttpServer stopped on port " + port);
        }
    }

    // 支付通知处理器
    static class PaymentNotifyHandler implements HttpHandler {
        private PayrecordDao payrecordDao = new PayrecordDao();

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
                System.out.println("📨 收到支付宝通知: " + requestBody);

                Map<String, Object> notifyData = JsonUtil.fromJsonToMap(requestBody);
                String tradeStatus = (String) notifyData.get("trade_status");
                String outTradeNo = (String) notifyData.get("out_trade_no");

                Payrecord payrecord = payrecordDao.findById(outTradeNo);
                if (payrecord != null) {
                    payrecord.setPayState(tradeStatus.equals("TRADE_SUCCESS") ? "已支付" : "未支付");
                    payrecord.setPayTime(LocalDateTime.now());
                    payrecordDao.save(payrecord);
                    SimpleHttpServer.sendJsonResponse(exchange, 200, Map.of("success", true, "message", "通知处理成功"));
                } else {
                    SimpleHttpServer.sendJsonResponse(exchange, 404, SimpleHttpServer.createErrorResponse("订单不存在"));
                }
            } catch (Exception e) {
                System.err.println("❌ 支付通知处理失败: " + e.getMessage());
                SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("通知处理失败: " + e.getMessage()));
            }
        }
    }
}