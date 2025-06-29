package dao;

import model.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // 保存用户（注册）- 身份证号作为主键
    public String save(User user) {
        // 设置注册时间
        if (user.getUserSignUpTime() == null) {
            user.setUserSignUpTime(LocalDateTime.now());
        }

        String sql = "INSERT INTO user (user_ID, user_password, user_name, user_gender, user_telephone, user_SignUpTime, VIPstate, user_authority) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUserId());        // 身份证号
            stmt.setString(2, user.getUserPassword());
            stmt.setString(3, user.getUserName());      // 真实姓名
            stmt.setString(4, user.getUserGender());
            stmt.setString(5, user.getUserTelephone());
            stmt.setTimestamp(6, Timestamp.valueOf(user.getUserSignUpTime())); // 注册时间
            stmt.setString(7, user.getVipState());
            stmt.setInt(8, user.getUserAuthority());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("保存用户失败");
            }

            return user.getUserId(); // 返回身份证号

        } catch (SQLException e) {
            System.err.println("保存用户失败: " + e.getMessage());
            throw new RuntimeException("保存用户失败", e);
        }
    }

    // 在现有的UserDao类中添加这个方法

    /**
     * 根据用户ID查找用户（用于VIP状态判断）
     * @param userId 用户ID
     * @return 用户对象，如果不存在返回null
     */
    public User findByUserId(String userId) {
        String sql = "SELECT * FROM user WHERE user_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("user_ID"));
                user.setUserPassword(rs.getString("user_password"));
                user.setUserName(rs.getString("user_name"));
                user.setUserGender(rs.getString("user_gender"));
                user.setUserTelephone(rs.getString("user_telephone"));
                user.setUserSignUpTime(rs.getTimestamp("user_SignUpTime").toLocalDateTime());
                user.setVipState(rs.getString("VIPstate"));
                user.setUserAuthority(rs.getInt("user_authority"));
                return user;
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 根据用户ID查询用户失败: " + e.getMessage());
            throw new RuntimeException("根据用户ID查询用户失败: " + e.getMessage());
        }
    }

    // 根据身份证号查找用户
    public User findById(String userId) {
        String sql = "SELECT * FROM user WHERE user_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId); // 身份证号

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("查询用户失败: " + e.getMessage());
            throw new RuntimeException("查询用户失败", e);
        }
    }

    // 用户登录验证（身份证号+密码）
    public User login(String userId, String password) {
        String sql = "SELECT * FROM user WHERE user_ID = ? AND user_password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);     // 身份证号
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("用户登录失败: " + e.getMessage());
            throw new RuntimeException("用户登录失败", e);
        }
    }

    // 检查身份证号是否已存在
    public boolean existsByUserId(String userId) {
        String sql = "SELECT COUNT(*) FROM user WHERE user_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId); // 身份证号

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("检查身份证号失败: " + e.getMessage());
            return false;
        }
    }

    // 根据姓名查找用户（可能有多个同名用户）
    public List<User> findByUserName(String userName) {
        String sql = "SELECT * FROM user WHERE user_name = ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userName); // 真实姓名

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("根据姓名查询用户失败: " + e.getMessage());
        }

        return users;
    }

    // 查询所有用户
    public List<User> findAll() {
        String sql = "SELECT * FROM user ORDER BY user_SignUpTime DESC LIMIT 100";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询用户列表失败: " + e.getMessage());
            throw new RuntimeException("查询用户列表失败", e);
        }

        return users;
    }

    // 将ResultSet转换为User对象
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("user_ID"));           // 身份证号
        user.setUserPassword(rs.getString("user_password"));
        user.setUserName(rs.getString("user_name"));       // 真实姓名
        user.setUserGender(rs.getString("user_gender"));
        user.setUserTelephone(rs.getString("user_telephone"));
        user.setUserSignUpTime(rs.getTimestamp("user_SignUpTime").toLocalDateTime()); // 注册时间
        user.setVipState(rs.getString("VIPstate"));
        user.setUserAuthority(rs.getInt("user_authority"));
        return user;
    }
}