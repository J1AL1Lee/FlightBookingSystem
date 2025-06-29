//黄闻远写的，有问题就去线下真实他
package dao;

import model.Order;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {
    // 保存订单
    public String save(Order order) {
        String sql = "INSERT INTO `order` (order_ID, user_ID, flight_ID, order_state, flight_time, order_time, seat_id, seat_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, order.getOrderId());
            stmt.setString(2, order.getUserId());
            stmt.setString(3, order.getFlightId());
            stmt.setString(4, order.getOrderState());
            stmt.setDate(5, java.sql.Date.valueOf(order.getFlightTime()));
            stmt.setTimestamp(6, Timestamp.valueOf(order.getOrderTime()));
            stmt.setInt(7, order.getSeatId());
            stmt.setInt(8, order.getSeatType());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("创建订单失败");
            return order.getOrderId();
        } catch (SQLException e) {
            System.err.println("创建订单失败: " + e.getMessage());
            throw new RuntimeException("创建订单失败", e);
        }
    }

    // 根据订单ID查询订单
    public Order findById(String orderId) {
        String sql = "SELECT * FROM `order` WHERE order_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getString("order_ID"));
                    order.setUserId(rs.getString("user_ID"));
                    order.setFlightId(rs.getString("flight_ID"));
                    order.setOrderState(rs.getString("order_state"));
                    order.setFlightTime(rs.getDate("flight_time").toLocalDate());
                    order.setOrderTime(rs.getTimestamp("order_time").toLocalDateTime());
                    order.setSeatId(rs.getInt("seat_id"));
                    order.setSeatType(rs.getInt("seat_type"));
                    return order;
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("查询订单失败: " + e.getMessage());
            throw new RuntimeException("查询订单失败", e);
        }
    }
}