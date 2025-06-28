package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/airdatabase?" +
            "useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&" +
            "allowPublicKeyRetrieval=true&useSSL=false";

    private static final String USERNAME = "root";  // 改成你的用户名
    private static final String PASSWORD = "43436Ljl"; // 改成你的密码

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ 数据库连接测试成功！");
        } catch (SQLException e) {
            System.err.println("❌ 数据库连接失败: " + e.getMessage());
            throw new RuntimeException("数据库连接失败", e);
        }
    }
}