package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.SimpleHttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceBasedStaticHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpServer.setCorsHeaders(exchange);

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
