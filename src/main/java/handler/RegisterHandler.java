package handler;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.UserDao;
import model.User;
import utils.JsonUtil;
import server.SimpleHttpServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public  class RegisterHandler implements HttpHandler {

    private UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("🔍 收到请求: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        // 设置CORS
        SimpleHttpServer.setCorsHeaders(exchange);

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
            SimpleHttpServer.sendJsonResponse(exchange, 405, SimpleHttpServer.createErrorResponse("只支持POST请求"));
            return;
        }

        try {
            // 读取请求体
            String requestBody = SimpleHttpServer.readRequestBody(exchange);
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
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("用户ID不能为空"));
                return;
            }
            if (userName == null || userName.trim().isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("姓名不能为空"));
                return;
            }
            if (userPassword == null || userPassword.trim().isEmpty()) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("密码不能为空"));
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
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("用户ID必须是6位数字，当前输入: " + userId));
                return;
            }

            // 验证密码长度
            if (userPassword.length() < 6) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("密码长度不能少于6位"));
                return;
            }

            // 验证用户名长度
            if (userName.length() < 2 || userName.length() > 50) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("姓名长度必须在2-50个字符之间"));
                return;
            }

            // 验证电话号码格式（8位数字，可选）
            if (userTelephone != null && !userTelephone.isEmpty()) {
                if (!userTelephone.matches("^[0-9]{8}$")) {
                    SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("电话号码必须是8位数字，当前输入: " + userTelephone));
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
                    SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("性别只能是'男'或'女'，当前收到: [" + userGender + "]"));
                    return;
                }
            }

            // 检查用户ID是否已存在
            if (userDao.existsByUserId(userId)) {
                SimpleHttpServer.sendJsonResponse(exchange, 400, SimpleHttpServer.createErrorResponse("用户ID " + userId + " 已注册"));
                return;
            }

            // 创建新用户
            User user = new User(userId, userPassword, userName,userTelephone);
            user.setUserGender(userGender);

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

            SimpleHttpServer.sendJsonResponse(exchange, 200, response);
            System.out.println("✅ 用户注册成功: " + userName + " (用户ID: " + savedUserId + ")");

        } catch (Exception e) {
            System.err.println("❌ 注册失败: " + e.getMessage());
            e.printStackTrace(); // 打印完整的错误堆栈，方便调试
            SimpleHttpServer.sendJsonResponse(exchange, 500, SimpleHttpServer.createErrorResponse("注册失败: " + e.getMessage()));
        }
    }
}
