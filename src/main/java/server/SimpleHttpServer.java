package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.DatabaseConnection;
import dao.UserDao;
import model.User;
import service.FlightSearchService;
import utils.JsonUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.File;
import service.BookingService;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.google.gson.Gson;
import dao.OrderDao;
import dao.PayrecordDao;
import model.Order;
import model.Payrecord;
import handler.*;

public class SimpleHttpServer {

    public static void main(String[] args) throws IOException {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        DatabaseConnection.testConnection();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // API路径
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/hello", new HelloHandler());
        server.createContext("/test", new TestHandler());
        server.createContext("/api/flights/search", new SimpleFlightSearchHandler());

        // 🎫 添加预订相关路由
        server.createContext("/api/booking/create", new BookingHandler());
        server.createContext("/api/booking/cancel", new BookingHandler());
        server.createContext("/api/booking/price", new BookingHandler());
        server.createContext("/api/booking/orders", new BookingHandler());
        server.createContext("/api/booking/refund", new BookingHandler());

        // 主支付相关路由，使用支付宝
        server.createContext("/api/payments/create", new PaymentCreateHandler());
        server.createContext("/api/payments/status", new PaymentStatusHandler());
        server.createContext("/api/payments/notify", new PaymentNotifyHandler());

        // 使用新的资源处理器
        server.createContext("/", new ResourceBasedStaticHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("🚀 服务器启动成功！");
        System.out.println("📍 访问登录页面: http://localhost:8080/sign_log.html");
        System.out.println("📍 当前工作目录: " + System.getProperty("user.dir"));
        System.out.println("📍 尝试读取: src/main/resources/static/sign_log.html");

        System.out.println("📍 订票相关 API:");
        System.out.println("   POST /api/booking/create - 创建订单");
        System.out.println("   POST /api/booking/cancel - 取消订单");
        System.out.println("   POST /api/booking/refund - 申请退款");
        System.out.println("   GET /api/booking/price - 查询价格");
        System.out.println("   GET /api/booking/orders - 查询用户订单");

        System.out.println("📍 支付宝支付 API:");
        System.out.println("   POST /api/payments/create - 发起支付（生成二维码）");
        System.out.println("   GET /api/payments/status - 查询支付状态");
        System.out.println("   POST /api/payments/notify - 接收支付宝通知");

        System.out.println("按 Ctrl+C 停止服务器");
    }

    // 工具方法 - 设置CORS头
    public static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // 工具方法 - 读取请求体
    public static String readRequestBody(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        System.out.println("🔍 Content-Type: " + contentType);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream is = exchange.getRequestBody()) {
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        }

        byte[] bodyBytes = buffer.toByteArray();
        System.out.println("🔍 原始字节数组: " + Arrays.toString(bodyBytes));

        String utf8Body = new String(bodyBytes, StandardCharsets.UTF_8);
        String iso88591Body = new String(bodyBytes, StandardCharsets.ISO_8859_1);

        System.out.println("🔍 UTF-8解码: " + utf8Body);
        System.out.println("🔍 ISO-8859-1解码: " + iso88591Body);

        if (utf8Body.contains("userName") && !utf8Body.contains("��")) {
            System.out.println("✅ 使用UTF-8解码");
            return utf8Body;
        } else if (iso88591Body.contains("userName")) {
            System.out.println("✅ 使用ISO-8859-1解码，需要重新编码为UTF-8");
            return new String(iso88591Body.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } else {
            System.out.println("⚠️ 使用默认UTF-8解码");
            return utf8Body;
        }
    }

    // 工具方法 - 发送JSON响应
    public static void sendJsonResponse(HttpExchange exchange, int code, Object data) throws IOException {
        String json = data instanceof String ? (String) data : JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // 工具方法 - 创建错误响应
    public static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }

    // 支付状态查询处理器
    static class PaymentStatusHandler implements HttpHandler {
        private PayrecordDao payrecordDao = new PayrecordDao();
        private AlipayClient alipayClient;

        public PaymentStatusHandler() {
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
                throw new RuntimeException("支付宝客户端初始化失败", e);
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, createErrorResponse("只支持GET请求"));
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
                    sendJsonResponse(exchange, 400, createErrorResponse("payId 不能为空"));
                    return;
                }

                AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
                request.setBizContent(new Gson().toJson(Map.of("out_trade_no", payId)));

                AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (response.isSuccess()) {
                    String tradeStatus = response.getTradeStatus();
                    Payrecord payrecord = payrecordDao.findById(payId);
                    if (payrecord != null) {
                        payrecord.setPayState(tradeStatus.equals("TRADE_SUCCESS") ? "已支付" : "未支付");
                        new PayrecordDao().save(payrecord); // 更新状态
                    }
                    sendJsonResponse(exchange, 200, Map.of("success", true, "payId", payId, "status", tradeStatus));
                } else {
                    sendJsonResponse(exchange, 500, createErrorResponse("查询支付状态失败: " + response.getMsg()));
                }
            } catch (AlipayApiException e) {
                System.err.println("❌ 支付宝查询异常: " + e.getMessage());
                sendJsonResponse(exchange, 500, createErrorResponse("支付宝查询失败: " + e.getMessage()));
            }
        }
    }

    // 支付通知处理器
    static class PaymentNotifyHandler implements HttpHandler {
        private PayrecordDao payrecordDao = new PayrecordDao();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, createErrorResponse("只支持POST请求"));
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);
                System.out.println("📨 收到支付宝通知: " + requestBody);

                Map<String, Object> notifyData = JsonUtil.fromJsonToMap(requestBody);
                String tradeStatus = (String) notifyData.get("trade_status");
                String outTradeNo = (String) notifyData.get("out_trade_no");

                Payrecord payrecord = payrecordDao.findById(outTradeNo);
                if (payrecord != null) {
                    payrecord.setPayState(tradeStatus.equals("TRADE_SUCCESS") ? "已支付" : "未支付");
                    payrecord.setPayTime(LocalDateTime.now());
                    new PayrecordDao().save(payrecord); // 更新状态
                    sendJsonResponse(exchange, 200, Map.of("success", true, "message", "通知处理成功"));
                } else {
                    sendJsonResponse(exchange, 404, createErrorResponse("订单不存在"));
                }
            } catch (Exception e) {
                System.err.println("❌ 支付通知处理失败: " + e.getMessage());
                sendJsonResponse(exchange, 500, createErrorResponse("通知处理失败: " + e.getMessage()));
            }
        }
    }
}