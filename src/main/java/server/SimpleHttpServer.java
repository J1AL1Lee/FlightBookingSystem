package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.DatabaseConnection;
import dao.UserDao;
import model.User;
import service.FlightSearchService;
import utils.JsonUtil;
import java.nio.file.Files;      // 添加这个
import java.nio.file.Path;       // 添加这个
import java.nio.file.Paths;      // 添加这个
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.File;

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
        // 添加简化的航班搜索路由
        server.createContext("/api/flights/search", new SimpleFlightSearchHandler());

        //主方法中的新路由，支付相关，by黄
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

        System.out.println("📍 支付宝 API 可用:");
        System.out.println("   POST /api/payments/create - 发起支付");
        System.out.println("   GET /api/payments/status - 查询支付状态");
        System.out.println("   POST /api/payments/notify - 接收支付宝通知");

        System.out.println("按 Ctrl+C 停止服务器");
    }

    // 新增：静态文件处理器
    static class ResourceBasedStaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();

            // 如果是根路径，重定向到登录页
            if ("/".equals(requestPath)) {
                requestPath = "/sign_log.html";
            }

            System.out.println("📂 请求文件: " + requestPath);

            try {
                // 方法1：尝试从classpath读取（编译后的资源）
                String resourcePath = "/static" + requestPath;
                InputStream resourceStream = getClass().getResourceAsStream(resourcePath);

                if (resourceStream == null) {
                    // 方法2：尝试从文件系统读取（开发时的源文件）
                    String[] possiblePaths = {
                            "src/main/resources/static" + requestPath,
                            "./src/main/resources/static" + requestPath,
                            "src\\main\\resources\\static" + requestPath.replace("/", "\\"),
                            ".\\src\\main\\resources\\static" + requestPath.replace("/", "\\")
                    };

                    for (String path : possiblePaths) {
                        try {
                            Path filePath = Paths.get(path);
                            System.out.println("🔍 尝试路径: " + filePath.toAbsolutePath());

                            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                                resourceStream = Files.newInputStream(filePath);
                                System.out.println("✅ 找到文件: " + path);
                                break;
                            }
                        } catch (Exception e) {
                            System.out.println("❌ 路径失败: " + path + " - " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("✅ 从classpath找到资源: " + resourcePath);
                }

                if (resourceStream != null) {
                    // 读取文件内容
                    byte[] content = resourceStream.readAllBytes();
                    resourceStream.close();

                    // 设置Content-Type
                    String contentType = getContentType(requestPath);
                    exchange.getResponseHeaders().set("Content-Type", contentType);

                    exchange.sendResponseHeaders(200, content.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(content);
                    }

                    System.out.println("✅ 成功返回文件: " + requestPath + " (" + content.length + " bytes)");
                } else {
                    // 文件完全找不到，显示详细调试信息
                    StringBuilder debugInfo = new StringBuilder();
                    debugInfo.append("404 - File Not Found: ").append(requestPath).append("\n\n");
                    debugInfo.append("当前工作目录: ").append(System.getProperty("user.dir")).append("\n");
                    debugInfo.append("Java classpath: ").append(System.getProperty("java.class.path")).append("\n\n");

                    debugInfo.append("尝试过的路径:\n");
                    debugInfo.append("1. Classpath: ").append(resourcePath).append("\n");

                    String[] possiblePaths = {
                            "src/main/resources/static" + requestPath,
                            "./src/main/resources/static" + requestPath,
                            "src\\main\\resources\\static" + requestPath.replace("/", "\\"),
                            ".\\src\\main\\resources\\static" + requestPath.replace("/", "\\")
                    };

                    for (int i = 0; i < possiblePaths.length; i++) {
                        Path path = Paths.get(possiblePaths[i]);
                        debugInfo.append(String.format("%d. %s -> %s (存在: %s)\n",
                                i + 2, possiblePaths[i], path.toAbsolutePath(), Files.exists(path)));
                    }

                    // 列出实际的static目录内容
                    String staticDir = "src/main/resources/static";
                    Path staticPath = Paths.get(staticDir);
                    debugInfo.append("\n").append(staticDir).append(" 目录信息:\n");
                    debugInfo.append("绝对路径: ").append(staticPath.toAbsolutePath()).append("\n");
                    debugInfo.append("目录存在: ").append(Files.exists(staticPath)).append("\n");

                    if (Files.exists(staticPath) && Files.isDirectory(staticPath)) {
                        debugInfo.append("目录内容:\n");
                        try {
                            Files.list(staticPath).forEach(p ->
                                    debugInfo.append("  - ").append(p.getFileName()).append("\n"));
                        } catch (Exception e) {
                            debugInfo.append("  无法列出目录: ").append(e.getMessage()).append("\n");
                        }
                    } else {
                        debugInfo.append("目录不存在或不是目录!\n");
                    }

                    String debugStr = debugInfo.toString();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(404, debugStr.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(debugStr.getBytes(StandardCharsets.UTF_8));
                    }

                    System.out.println("❌ 文件完全找不到: " + requestPath);
                    System.out.println(debugStr);
                }
            } catch (Exception e) {
                System.err.println("❌ 处理文件失败: " + e.getMessage());
                e.printStackTrace();

                String error = "500 - Internal Server Error: " + e.getMessage();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(500, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".css")) return "text/css; charset=utf-8";
            if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (path.endsWith(".json")) return "application/json; charset=utf-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".gif")) return "image/gif";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=utf-8";
        }
    }

// 还需要添加 File 的 import
// 在文件顶部添加：


    // 用户注册处理器
    // 用户注册处理器
    static class RegisterHandler implements HttpHandler {

        private UserDao userDao = new UserDao();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("🔍 收到请求: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

            // 设置CORS
            setCorsHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                System.out.println("✅ 处理OPTIONS预检请求");
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, createErrorResponse("只支持POST请求"));
                return;
            }

            try {
                // 读取请求体
                String requestBody = readRequestBody(exchange);
                System.out.println("📨 收到注册请求: " + requestBody);

                // 解析JSON
                Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
                String userId = (String) requestData.get("userId");           // 用户ID（6位数字）
                String userName = (String) requestData.get("userName");       // 真实姓名
                String userPassword = (String) requestData.get("userPassword");
                String userGender = (String) requestData.get("userGender");
                String userTelephone = (String) requestData.get("userTelephone");

                // 添加调试信息
                System.out.println("🔍 解析结果:");
                System.out.println("   用户ID: [" + userId + "]");
                System.out.println("   用户名: [" + userName + "]");
                System.out.println("   性别: [" + userGender + "]");
                System.out.println("   电话: [" + userTelephone + "]");

                // 验证必填字段
                if (userId == null || userId.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, createErrorResponse("用户ID不能为空"));
                    return;
                }
                if (userName == null || userName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, createErrorResponse("姓名不能为空"));
                    return;
                }
                if (userPassword == null || userPassword.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, createErrorResponse("密码不能为空"));
                    return;
                }

                // 去除前后空格
                userId = userId.trim();
                userName = userName.trim();
                userPassword = userPassword.trim();
                if (userGender != null) userGender = userGender.trim();
                if (userTelephone != null) userTelephone = userTelephone.trim();

                // 验证用户ID格式（6位数字）
                if (!userId.matches("^[0-9]{6}$")) {
                    sendJsonResponse(exchange, 400, createErrorResponse("用户ID必须是6位数字，当前输入: " + userId));
                    return;
                }

                // 验证密码长度
                if (userPassword.length() < 6) {
                    sendJsonResponse(exchange, 400, createErrorResponse("密码长度不能少于6位"));
                    return;
                }

                // 验证用户名长度
                if (userName.length() < 2 || userName.length() > 50) {
                    sendJsonResponse(exchange, 400, createErrorResponse("姓名长度必须在2-50个字符之间"));
                    return;
                }

                // 验证电话号码格式（8位数字，可选）
                if (userTelephone != null && !userTelephone.isEmpty()) {
                    if (!userTelephone.matches("^[0-9]{8}$")) {
                        sendJsonResponse(exchange, 400, createErrorResponse("电话号码必须是8位数字，当前输入: " + userTelephone));
                        return;
                    }
                }

                // 验证性别（可选）
                if (userGender != null && !userGender.isEmpty()) {
                    if (!userGender.equals("男") && !userGender.equals("女")) {
                        System.out.println("❌ 性别验证失败，收到: [" + userGender + "], 长度: " + userGender.length());
                        // 打印每个字符的Unicode编码，帮助调试中文问题
                        for (int i = 0; i < userGender.length(); i++) {
                            char c = userGender.charAt(i);
                            System.out.println("   字符 " + i + ": [" + c + "] Unicode: " + (int)c);
                        }
                        sendJsonResponse(exchange, 400, createErrorResponse("性别只能是'男'或'女'，当前收到: [" + userGender + "]"));
                        return;
                    }
                }

                // 检查用户ID是否已存在
                if (userDao.existsByUserId(userId)) {
                    sendJsonResponse(exchange, 400, createErrorResponse("用户ID " + userId + " 已注册"));
                    return;
                }

                // 创建新用户
                User user = new User(userId, userPassword, userName);
                user.setUserGender(userGender);
                user.setUserTelephone(userTelephone);

                System.out.println("💾 准备保存用户: " + user.toString());

                String savedUserId = userDao.save(user);

                // 返回成功响应（不返回密码）
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "注册成功");
                response.put("userId", savedUserId);
                response.put("userName", userName);
                response.put("userGender", userGender);
                response.put("userTelephone", userTelephone);
                response.put("vipState", user.getVipState());
                response.put("signUpTime", user.getUserSignUpTime().toString());

                sendJsonResponse(exchange, 200, response);
                System.out.println("✅ 用户注册成功: " + userName + " (用户ID: " + savedUserId + ")");

            } catch (Exception e) {
                System.err.println("❌ 注册失败: " + e.getMessage());
                e.printStackTrace(); // 打印完整的错误堆栈，方便调试
                sendJsonResponse(exchange, 500, createErrorResponse("注册失败: " + e.getMessage()));
            }
        }
    }

    // 用户登录处理器
    static class LoginHandler implements HttpHandler {
        private UserDao userDao = new UserDao();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, createErrorResponse("只支持POST请求"));
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);
                System.out.println("📨 收到登录请求: " + requestBody);

                Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
                String userId = (String) requestData.get("userId");
                String userPassword = (String) requestData.get("userPassword");

                if (userId == null || userPassword == null) {
                    sendJsonResponse(exchange, 400, createErrorResponse("用户ID和密码不能为空"));
                    return;
                }

                User user = userDao.login(userId, userPassword);
                if (user != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "登录成功");
                    response.put("userId", user.getUserId());
                    response.put("userName", user.getUserName());
                    response.put("vipState", user.getVipState());
                    response.put("userAuthority", user.getUserAuthority());

                    sendJsonResponse(exchange, 200, response);
                    System.out.println("✅ 用户登录成功: " + user.getUserName() + " (" + user.getUserId() + ")");
                } else {
                    sendJsonResponse(exchange, 401, createErrorResponse("用户ID或密码错误"));
                }

            } catch (Exception e) {
                System.err.println("❌ 登录失败: " + e.getMessage());
                sendJsonResponse(exchange, 500, createErrorResponse("登录失败: " + e.getMessage()));
            }
        }
    }

    // 用户列表处理器
    static class UsersHandler implements HttpHandler {
        private UserDao userDao = new UserDao();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, createErrorResponse("只支持GET请求"));
                return;
            }

            try {
                List<User> users = userDao.findAll();

                // 不返回密码信息
                List<Map<String, Object>> userList = new ArrayList<>();
                for (User user : users) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", user.getUserId());
                    userInfo.put("userName", user.getUserName());
                    userInfo.put("userGender", user.getUserGender());
                    userInfo.put("userTelephone", user.getUserTelephone());
                    userInfo.put("vipState", user.getVipState());
                    userInfo.put("signUpTime", user.getUserSignUpTime().toString());
                    userList.add(userInfo);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", userList);
                response.put("count", users.size());

                sendJsonResponse(exchange, 200, response);
                System.out.println("📊 返回用户列表，共 " + users.size() + " 个用户");

            } catch (Exception e) {
                System.err.println("❌ 获取用户列表失败: " + e.getMessage());
                sendJsonResponse(exchange, 500, createErrorResponse("获取用户列表失败"));
            }
        }
    }

    //简化版航班搜索API处理器 - 只支持基础的起降地查询

    static class SimpleFlightSearchHandler implements HttpHandler {
        private FlightSearchService searchService = new FlightSearchService();

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
                System.out.println("🔍 收到航班搜索请求: " + requestBody);

                Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);

                // 获取基本参数
                String airportFrom = (String) requestData.get("airportFrom");
                String airportTo = (String) requestData.get("airportTo");
                String flightDate = (String) requestData.get("flightDate");
                String userId = (String) requestData.get("userId"); // 可选，用于VIP判断

                // 参数验证
                if (airportFrom == null || airportTo == null || flightDate == null) {
                    sendJsonResponse(exchange, 400, createErrorResponse("缺少必需参数：airportFrom, airportTo, flightDate"));
                    return;
                }

                if (airportFrom.equals(airportTo)) {
                    sendJsonResponse(exchange, 400, createErrorResponse("出发机场和到达机场不能相同"));
                    return;
                }

                try {
                    LocalDate date = LocalDate.parse(flightDate);

                    // 调用搜索服务
                    List<FlightSearchService.FlightSearchResult> results =
                            searchService.searchAvailableFlights(airportFrom, airportTo, date, userId);

                    // 转换为前端友好的格式
                    List<Map<String, Object>> flightList = new ArrayList<>();
                    for (FlightSearchService.FlightSearchResult result : results) {
                        Map<String, Object> flightData = new HashMap<>();

                        // 基本信息
                        flightData.put("flightId", result.getFlightId());
                        flightData.put("airportFrom", result.getAirportFrom());
                        flightData.put("airportTo", result.getAirportTo());
                        flightData.put("flightDate", result.getFlightDate().toString());
                        flightData.put("timeTakeoff", result.getTimeTakeoff().toString());
                        flightData.put("timeArrive", result.getTimeArrive().toString());

                        // 航空公司信息
                        flightData.put("airlineName", result.getAirlineName());

                        // 座位信息
                        flightData.put("seat0Left", result.getSeat0Left());
                        flightData.put("seat1Left", result.getSeat1Left());

                        // 价格信息（根据VIP状态显示）
                        flightData.put("finalPrice0", result.getFinalPrice0());
                        flightData.put("finalPrice1", result.getFinalPrice1());
                        flightData.put("isVipUser", result.isVipUser());

                        // 如果是VIP用户且有折扣，显示折扣信息
                        if (result.isVipUser() && result.getDiscount() < 1.0f) {
                            flightData.put("originalPrice0", result.getOriginalPrice0());
                            flightData.put("originalPrice1", result.getOriginalPrice1());
                            flightData.put("discount", result.getDiscount());
                            flightData.put("hasDiscount", true);
                        } else {
                            flightData.put("hasDiscount", false);
                        }

                        // 航程记录ID（用于后续预订）
                        flightData.put("flightrecordId", result.getFlightrecord().getFlightrecordId());

                        flightList.add(flightData);
                    }

                    // 返回结果
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", flightList);
                    response.put("count", results.size());
                    response.put("message", "搜索成功，找到 " + results.size() + " 个航班");

                    sendJsonResponse(exchange, 200, response);

                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, createErrorResponse("日期格式错误或搜索失败: " + e.getMessage()));
                }

            } catch (Exception e) {
                System.err.println("❌ 航班搜索API处理失败: " + e.getMessage());
                e.printStackTrace();
                sendJsonResponse(exchange, 500, createErrorResponse("搜索失败: " + e.getMessage()));
            }
        }
    }

    // Hello接口处理器
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello! 航空订票系统运行正常 ✈️\n\n可用API:\n- POST /api/register - 用户注册\n- POST /api/login - 用户登录\n- GET /api/users - 用户列表";

            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }

    // 测试接口处理器
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            try {
                DatabaseConnection.testConnection();
                response = JsonUtil.toJson(Map.of(
                        "status", "success",
                        "message", "数据库连接正常",
                        "timestamp", new Date().toString()
                ));
            } catch (Exception e) {
                response = JsonUtil.toJson(createErrorResponse("数据库连接失败: " + e.getMessage()));
            }

            sendJsonResponse(exchange, 200, response);
        }
    }

    // 工具方法 - 设置CORS头
    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // 工具方法 - 读取请求体
    // 修改readRequestBody方法，确保使用UTF-8
    // 修改readRequestBody方法，强制处理UTF-8
    // 简化的readRequestBody方法
    // 修改SimpleHttpServer.java中的readRequestBody方法
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        // 尝试从Content-Type头获取字符集
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        System.out.println("🔍 Content-Type: " + contentType);

        // 读取原始字节
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

        // 尝试不同的字符编码
        String utf8Body = new String(bodyBytes, StandardCharsets.UTF_8);
        String iso88591Body = new String(bodyBytes, StandardCharsets.ISO_8859_1);

        System.out.println("🔍 UTF-8解码: " + utf8Body);
        System.out.println("🔍 ISO-8859-1解码: " + iso88591Body);

        // 检测哪个解码结果包含正确的中文
        if (utf8Body.contains("userName") && !utf8Body.contains("��")) {
            System.out.println("✅ 使用UTF-8解码");
            return utf8Body;
        } else if (iso88591Body.contains("userName")) {
            System.out.println("✅ 使用ISO-8859-1解码，需要重新编码为UTF-8");
            // 重新编码：ISO-8859-1字节 -> UTF-8字符串
            return new String(iso88591Body.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } else {
            System.out.println("⚠️ 使用默认UTF-8解码");
            return utf8Body;
        }
    }

    // 修改sendJsonResponse方法
    private static void sendJsonResponse(HttpExchange exchange, int code, Object data) throws IOException {
        String json = data instanceof String ? (String) data : JsonUtil.toJson(data);

        // 确保使用UTF-8编码
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }


    // 新增：支付创建处理器
    static class PaymentCreateHandler implements HttpHandler {
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
                System.out.println("📨 收到支付请求: " + requestBody);

                Map<String, Object> requestData = JsonUtil.fromJsonToMap(requestBody);
                String orderId = (String) requestData.get("orderId");
                if (orderId == null || orderId.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, createErrorResponse("orderId 不能为空"));
                    return;
                }

                Order order = orderDao.findById(orderId);
                if (order == null) {
                    sendJsonResponse(exchange, 404, createErrorResponse("订单不存在"));
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
                    sendJsonResponse(exchange, 200, responseData);
                    System.out.println("✅ 支付创建成功: " + orderId);
                } else {
                    sendJsonResponse(exchange, 500, createErrorResponse("支付创建失败: " + response.getMsg()));
                }
            } catch (AlipayApiException e) {
                System.err.println("❌ 支付宝 API 异常: " + e.getMessage());
                sendJsonResponse(exchange, 500, createErrorResponse("支付宝调用失败: " + e.getMessage()));
            } catch (Exception e) {
                System.err.println("❌ 支付处理失败: " + e.getMessage());
                sendJsonResponse(exchange, 500, createErrorResponse("支付处理失败: " + e.getMessage()));
            }
        }
    }

    // 新增：支付状态查询处理器
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
                throw new RuntimeException("支付宝客户端初始化失败", e); // 转换为 RuntimeException
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

    // 新增：支付通知处理器
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

    // 工具方法 - 创建错误响应
    private static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}