//黄闻远写的，有问题就去线下真实他
package dao;

import model.Payrecord;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PayrecordDao {
    // 保存支付记录
    public String save(Payrecord payrecord) {
        String sql = "INSERT INTO payrecord (pay_ID, order_ID, payment, pay_method, pay_state, pay_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payrecord.getPayId());
            stmt.setString(2, payrecord.getOrderId());
            stmt.setInt(3, payrecord.getPayment());
            stmt.setString(4, payrecord.getPayMethod());
            stmt.setString(5, payrecord.getPayState());
            stmt.setTimestamp(6, Timestamp.valueOf(payrecord.getPayTime()));
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("保存支付记录失败");
            return payrecord.getPayId();
        } catch (SQLException e) {
            System.err.println("保存支付记录失败: " + e.getMessage());
            throw new RuntimeException("保存支付记录失败", e);
        }
    }

    // 根据支付ID查询支付记录
    public Payrecord findById(String payId) {
        String sql = "SELECT * FROM payrecord WHERE pay_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Payrecord payrecord = new Payrecord();
                    payrecord.setPayId(rs.getString("pay_ID"));
                    payrecord.setOrderId(rs.getString("order_ID"));
                    payrecord.setPayment(rs.getInt("payment"));
                    payrecord.setPayMethod(rs.getString("pay_method"));
                    payrecord.setPayState(rs.getString("pay_state"));
                    payrecord.setPayTime(rs.getTimestamp("pay_time").toLocalDateTime());
                    return payrecord;
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("查询支付记录失败: " + e.getMessage());
            throw new RuntimeException("查询支付记录失败", e);
        }
    }

    // 更新支付状态
    public void updateStatus(String payId, String status) {
        String sql = "UPDATE payrecord SET pay_state = ?, pay_time = ? WHERE pay_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, payId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("更新支付状态失败");
        } catch (SQLException e) {
            System.err.println("更新支付状态失败: " + e.getMessage());
            throw new RuntimeException("更新支付状态失败", e);
        }
    }
}