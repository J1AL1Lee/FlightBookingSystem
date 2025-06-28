package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dao.DatabaseConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SimpleHttpServer {

    public static void main(String[] args) throws IOException {
        // 测试数据库连接
        DatabaseConnection.testConnection();

        // 创建HTTP服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 添加路由
        server.createContext("/hello", new HelloHandler());
        server.createContext("/test", new TestHandler());

        // 启动服务器
        server.setExecutor(null);
        server.start();

        System.out.println("🚀 服务器启动成功！");
        System.out.println("📍 访问地址:");
        System.out.println("   http://localhost:8080/hello");
        System.out.println("   http://localhost:8080/test");
        System.out.println("按 Ctrl+C 停止服务器");
    }

    // Hello接口处理器
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello! 航空订票系统运行正常 ✈️";

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
            // 测试数据库连接
            String response;
            try {
                DatabaseConnection.testConnection();
                response = "{\n  \"status\": \"success\",\n  \"message\": \"数据库连接正常\",\n  \"timestamp\": \"" + new java.util.Date() + "\"\n}";
            } catch (Exception e) {
                response = "{\n  \"status\": \"error\",\n  \"message\": \"" + e.getMessage() + "\"\n}";
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
}