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

        // 🔐 用户认证相关路由
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/logout", new LogoutHandler());           // 新增：退出登录
        server.createContext("/api/user/current", new CurrentUserHandler()); // 新增：获取当前用户信息

        // 👥 用户管理路由
        server.createContext("/api/users", new UsersHandler());

        // 🛩️ 航班搜索路由
        server.createContext("/api/flights/search", new SimpleFlightSearchHandler());

        // 🎫 订票相关路由
        server.createContext("/api/booking/create", new BookingHandler());
        server.createContext("/api/booking/cancel", new BookingHandler());
        server.createContext("/api/booking/price", new BookingHandler());
        server.createContext("/api/booking/orders", new BookingHandler());
        server.createContext("/api/booking/refund", new BookingHandler());

        //管理员路径
        server.createContext("/api/admin/flight/add", new AddFlightHandler());
        server.createContext("/api/admin/flight/all", new GetAllFlightsHandler());
        //server.createContext("/api/admin/user/authority", new ModifyUserAuthorityHandler());
        //server.createContext("/api/admin/db/query", new DatabaseQueryHandler());

        // 💰 支付相关路由
        server.createContext("/api/payments/create", new PaymentCreateHandler());
        server.createContext("/api/payments/status", new PaymentStatusHandler());
        server.createContext("/api/payments/notify", new PaymentNotifyHandler());

        // 🧪 测试路由
        server.createContext("/hello", new HelloHandler());
        server.createContext("/test", new TestHandler());

        // 📄 静态资源路由（必须放在最后）
        server.createContext("/", new ResourceBasedStaticHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("🚀 服务器启动成功！");
        System.out.println("📍 访问登录页面: http://localhost:8080/sign_log.html");
        System.out.println("📍 当前工作目录: " + System.getProperty("user.dir"));
        System.out.println("📍 尝试读取: src/main/resources/static/sign_log.html");

        System.out.println("\n🔐 用户认证 API:");
        System.out.println("   POST /api/login - 用户登录");
        System.out.println("   POST /api/logout - 用户退出");
        System.out.println("   POST /api/register - 用户注册");
        System.out.println("   GET /api/user/current - 获取当前登录用户信息");

        System.out.println("\n🛩️ 航班搜索 API:");
        System.out.println("   POST /api/flights/search - 搜索航班");

        System.out.println("\n🎫 订票相关 API:");
        System.out.println("   POST /api/booking/create - 创建订单");
        System.out.println("   POST /api/booking/cancel - 取消订单");
        System.out.println("   POST /api/booking/refund - 申请退款");
        System.out.println("   GET /api/booking/price - 查询价格");
        System.out.println("   GET /api/booking/orders - 查询用户订单");

        System.out.println("\n💰 支付宝支付 API:");
        System.out.println("   POST /api/payments/create - 发起支付（生成二维码）");
        System.out.println("   GET /api/payments/status - 查询支付状态");
        System.out.println("   POST /api/payments/notify - 接收支付宝通知");

        // 🧹 启动session清理任务（可选）
        startSessionCleanupTask();

        System.out.println("\n按 Ctrl+C 停止服务器");
    }

    /**
     * 启动定期清理过期session的任务
     */
    private static void startSessionCleanupTask() {
        Timer timer = new Timer("SessionCleanup", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LoginHandler.cleanExpiredSessions();
            }
        }, 60000, 300000); // 1分钟后开始，每5分钟执行一次

        System.out.println("🧹 Session清理任务已启动");
    }

    // 工具方法 - 设置CORS头
    public static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Cookie");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true"); // 重要：允许发送Cookie
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

    // 工具方法 - 验证用户是否已登录
    public static Map<String, Object> getCurrentUser(HttpExchange exchange) {
        String sessionId = getSessionId(exchange);
        if (sessionId != null && LoginHandler.sessions.containsKey(sessionId)) {
            return LoginHandler.sessions.get(sessionId);
        }
        return null;
    }

    // 工具方法 - 从请求中获取Session ID
    public static String getSessionId(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            String[] cookies = cookie.split(";");
            for (String c : cookies) {
                String[] parts = c.trim().split("=");
                if (parts.length == 2 && "SESSIONID".equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    // 工具方法 - 检查是否需要认证的API
    public static boolean needsAuth(String path) {
        return path.startsWith("/api/booking") ||
                path.startsWith("/api/orders") ||
                path.startsWith("/api/user/") ||
                path.startsWith("/api/payments");
    }

    // 工具方法 - 发送未认证响应
    public static void sendUnauthorizedResponse(HttpExchange exchange) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "未登录");
        sendJsonResponse(exchange, 401, response);
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