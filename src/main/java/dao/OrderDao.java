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
            System.out.println("✅ 订单保存成功: " + order.getOrderId());
            return order.getOrderId();
        } catch (SQLException e) {
            System.err.println("❌ 创建订单失败: " + e.getMessage());
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
            System.err.println("❌ 查询订单失败: " + e.getMessage());
            throw new RuntimeException("查询订单失败", e);
        }
    }

    // 更新订单状态
    public boolean updateOrderState(String orderId, String newState) {
        String sql = "UPDATE `order` SET order_state = ? WHERE order_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newState);
            stmt.setString(2, orderId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ 订单状态更新成功: " + orderId + " -> " + newState);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ 更新订单状态失败: " + e.getMessage());
            throw new RuntimeException("更新订单状态失败", e);
        }
    }

    // 根据用户ID查询所有订单
    public List<Order> findByUserId(String userId) {
        String sql = "SELECT * FROM `order` WHERE user_ID = ? ORDER BY order_time DESC";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    orders.add(order);
                }
            }
            System.out.println("🔍 查询用户订单: " + userId + "，找到 " + orders.size() + " 个订单");
            return orders;
        } catch (SQLException e) {
            System.err.println("❌ 查询用户订单失败: " + e.getMessage());
            throw new RuntimeException("查询用户订单失败", e);
        }
    }

    // 删除订单（取消订单时使用）
    public boolean deleteOrder(String orderId) {
        String sql = "DELETE FROM `order` WHERE order_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ 订单删除成功: " + orderId);
                return true;
            }
            System.out.println("⚠️ 未找到要删除的订单: " + orderId);
            return false;
        } catch (SQLException e) {
            System.err.println("❌ 删除订单失败: " + e.getMessage());
            throw new RuntimeException("删除订单失败", e);
        }
    }

    // 检查订单是否属于指定用户
    public boolean isOrderOwnedByUser(String orderId, String userId) {
        String sql = "SELECT COUNT(*) FROM `order` WHERE order_ID = ? AND user_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ 验证订单归属失败: " + e.getMessage());
            return false;
        }
    }

    // 根据航班ID和日期查询订单（统计用）
    public List<Order> findByFlightAndDate(String flightId, LocalDate flightDate) {
        String sql = "SELECT * FROM `order` WHERE flight_ID = ? AND flight_time = ?";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, flightId);
            stmt.setDate(2, java.sql.Date.valueOf(flightDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    orders.add(order);
                }
            }
            return orders;
        } catch (SQLException e) {
            System.err.println("❌ 查询航班订单失败: " + e.getMessage());
            throw new RuntimeException("查询航班订单失败", e);
        }
    }

    // 根据订单状态查询订单
    public List<Order> findByOrderState(String orderState) {
        String sql = "SELECT * FROM `order` WHERE order_state = ? ORDER BY order_time DESC";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderState);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    orders.add(order);
                }
            }
            System.out.println("🔍 查询状态为 '" + orderState + "' 的订单，找到 " + orders.size() + " 个");
            return orders;
        } catch (SQLException e) {
            System.err.println("❌ 根据状态查询订单失败: " + e.getMessage());
            throw new RuntimeException("根据状态查询订单失败", e);
        }
    }

    // 查询用户在指定日期范围内的订单
    public List<Order> findByUserIdAndDateRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM `order` WHERE user_ID = ? AND order_time BETWEEN ? AND ? ORDER BY order_time DESC";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));
            stmt.setTimestamp(3, Timestamp.valueOf(endTime));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    orders.add(order);
                }
            }
            return orders;
        } catch (SQLException e) {
            System.err.println("❌ 查询日期范围内订单失败: " + e.getMessage());
            throw new RuntimeException("查询日期范围内订单失败", e);
        }
    }

    // 统计用户订单数量
    public int countOrdersByUserId(String userId) {
        String sql = "SELECT COUNT(*) FROM `order` WHERE user_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ 统计订单数量失败: " + e.getMessage());
            return 0;
        }
    }

    // 检查订单ID是否已存在
    public boolean isOrderIdExists(String orderId) {
        String sql = "SELECT COUNT(*) FROM `order` WHERE order_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ 检查订单ID失败: " + e.getMessage());
            return false;
        }
    }

    // 批量更新订单状态（管理员功能）
    public int batchUpdateOrderState(List<String> orderIds, String newState) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE `order` SET order_state = ? WHERE order_ID = ?";
        int successCount = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // 开启事务

            for (String orderId : orderIds) {
                stmt.setString(1, newState);
                stmt.setString(2, orderId);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit(); // 提交事务

            for (int result : results) {
                if (result > 0) successCount++;
            }

            System.out.println("✅ 批量更新订单状态完成: " + successCount + "/" + orderIds.size() + " 成功");
            return successCount;

        } catch (SQLException e) {
            System.err.println("❌ 批量更新订单状态失败: " + e.getMessage());
            throw new RuntimeException("批量更新订单状态失败", e);
        }
    }

    // 查询超时未支付的订单（用于自动取消）
    public List<Order> findTimeoutUnpaidOrders(int timeoutMinutes) {
        String sql = "SELECT * FROM `order` WHERE order_state = '未支付' " +
                "AND order_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, timeoutMinutes);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    orders.add(order);
                }
            }
            System.out.println("🔍 查询超时未支付订单，找到 " + orders.size() + " 个");
            return orders;
        } catch (SQLException e) {
            System.err.println("❌ 查询超时订单失败: " + e.getMessage());
            throw new RuntimeException("查询超时订单失败", e);
        }
    }

    // 私有辅助方法：将ResultSet映射为Order对象
    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
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
}