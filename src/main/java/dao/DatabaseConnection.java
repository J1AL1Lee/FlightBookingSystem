package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // 修复URL参数连接，确保每个参数之间有&分隔
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/airdatabase?" +
            "useUnicode=true&" +
            "characterEncoding=UTF-8&" +
            "useSSL=false&" +  // 这里要用false，不是FALSE
            "serverTimezone=Asia/Shanghai&" +
            "allowPublicKeyRetrieval=true&" +
            "autoReconnect=true&" +
            "characterSetResults=UTF-8&" +
            "zeroDateTimeBehavior=convertToNull";

    private static final String USERNAME = "root";
    private static final String PASSWORD = "43436Ljl"; // 改成你的实际密码

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ 数据库连接测试成功！");
            System.out.println("📊 连接信息: " + conn.getMetaData().getDatabaseProductName() +
                    " " + conn.getMetaData().getDatabaseProductVersion());

            // 测试中文字符
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT '测试中文' as test_chinese")) {
                if (rs.next()) {
                    System.out.println("✅ 中文字符测试: " + rs.getString("test_chinese"));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ 数据库连接失败: " + e.getMessage());
            System.err.println("💡 请检查:");
            System.err.println("   1. MySQL服务是否启动");
            System.err.println("   2. 数据库flight_booking是否存在");
            System.err.println("   3. 用户名密码是否正确: " + USERNAME);
            System.err.println("   4. 当前连接URL: " + URL);
            throw new RuntimeException("数据库连接失败", e);
        }
    }
}