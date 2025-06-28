package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.DatabaseConnection;
import dao.UserDao;
import model.User;
import utils.JsonUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class SimpleHttpServer {

    public static void main(String[] args) throws IOException {

        // 设置系统属性
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        // 测试数据库连接
        DatabaseConnection.testConnection();

        // 创建HTTP服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 添加路由
        server.createContext("/hello", new HelloHandler());
        server.createContext("/test", new TestHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/users", new UsersHandler());

        // 启动服务器
        server.setExecutor(null);
        server.start();

        System.out.println("🚀 服务器启动成功！");
        System.out.println("📍 可用接口:");
        System.out.println("   GET  http://localhost:8080/hello");
        System.out.println("   GET  http://localhost:8080/test");
        System.out.println("   POST http://localhost:8080/api/register");
        System.out.println("   POST http://localhost:8080/api/login");
        System.out.println("   GET  http://localhost:8080/api/users");
        System.out.println("🎯 试试用户注册:");
        System.out.println("   curl -X POST http://localhost:8080/api/register -H \"Content-Type: application/json\" -d '{\"userId\":\"123456\",\"userName\":\"张三\",\"userPassword\":\"123456\"}'");
        System.out.println("按 Ctrl+C 停止服务器");
    }

    // 用户注册处理器
    // 用户注册处理器
    static class RegisterHandler implements HttpHandler {
        private UserDao userDao = new UserDao();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 设置CORS
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

    // 工具方法 - 创建错误响应
    private static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}