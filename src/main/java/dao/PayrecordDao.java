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
            System.out.println("✅ 支付记录保存成功: " + payrecord.getPayId());
            return payrecord.getPayId();
        } catch (SQLException e) {
            System.err.println("❌ 保存支付记录失败: " + e.getMessage());
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
                    return mapResultSetToPayrecord(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("❌ 查询支付记录失败: " + e.getMessage());
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
            System.out.println("✅ 支付状态更新成功: " + payId + " -> " + status);
        } catch (SQLException e) {
            System.err.println("❌ 更新支付状态失败: " + e.getMessage());
            throw new RuntimeException("更新支付状态失败", e);
        }
    }

    // 根据订单ID查询支付记录
    public Payrecord findByOrderId(String orderId) {
        String sql = "SELECT * FROM payrecord WHERE order_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayrecord(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("❌ 根据订单ID查询支付记录失败: " + e.getMessage());
            throw new RuntimeException("根据订单ID查询支付记录失败", e);
        }
    }

    // 查询指定用户的所有支付记录（通过订单关联）
    public List<Payrecord> findByUserId(String userId) {
        String sql = "SELECT p.* FROM payrecord p " +
                "INNER JOIN `order` o ON p.order_ID = o.order_ID " +
                "WHERE o.user_ID = ? ORDER BY p.pay_time DESC";
        List<Payrecord> payrecords = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Payrecord payrecord = mapResultSetToPayrecord(rs);
                    payrecords.add(payrecord);
                }
            }
            System.out.println("🔍 查询用户支付记录: " + userId + "，找到 " + payrecords.size() + " 条记录");
            return payrecords;
        } catch (SQLException e) {
            System.err.println("❌ 查询用户支付记录失败: " + e.getMessage());
            throw new RuntimeException("查询用户支付记录失败", e);
        }
    }

    // 根据支付状态查询支付记录
    public List<Payrecord> findByPayState(String payState) {
        String sql = "SELECT * FROM payrecord WHERE pay_state = ? ORDER BY pay_time DESC";
        List<Payrecord> payrecords = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payState);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Payrecord payrecord = mapResultSetToPayrecord(rs);
                    payrecords.add(payrecord);
                }
            }
            System.out.println("🔍 查询支付状态为 '" + payState + "' 的记录，找到 " + payrecords.size() + " 条");
            return payrecords;
        } catch (SQLException e) {
            System.err.println("❌ 根据支付状态查询失败: " + e.getMessage());
            throw new RuntimeException("根据支付状态查询失败", e);
        }
    }

    // 根据支付方式查询支付记录
    public List<Payrecord> findByPayMethod(String payMethod) {
        String sql = "SELECT * FROM payrecord WHERE pay_method = ? ORDER BY pay_time DESC";
        List<Payrecord> payrecords = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payMethod);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Payrecord payrecord = mapResultSetToPayrecord(rs);
                    payrecords.add(payrecord);
                }
            }
            return payrecords;
        } catch (SQLException e) {
            System.err.println("❌ 根据支付方式查询失败: " + e.getMessage());
            throw new RuntimeException("根据支付方式查询失败", e);
        }
    }

    // 查询指定时间范围内的支付记录
    public List<Payrecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM payrecord WHERE pay_time BETWEEN ? AND ? ORDER BY pay_time DESC";
        List<Payrecord> payrecords = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startTime));
            stmt.setTimestamp(2, Timestamp.valueOf(endTime));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Payrecord payrecord = mapResultSetToPayrecord(rs);
                    payrecords.add(payrecord);
                }
            }
            return payrecords;
        } catch (SQLException e) {
            System.err.println("❌ 查询时间范围内支付记录失败: " + e.getMessage());
            throw new RuntimeException("查询时间范围内支付记录失败", e);
        }
    }

    // 检查支付ID是否已存在
    public boolean isPayIdExists(String payId) {
        String sql = "SELECT COUNT(*) FROM payrecord WHERE pay_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ 检查支付ID失败: " + e.getMessage());
            return false;
        }
    }

    // 检查订单是否已有支付记录
    public boolean hasPaymentRecord(String orderId) {
        String sql = "SELECT COUNT(*) FROM payrecord WHERE order_ID = ?";
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
            System.err.println("❌ 检查订单支付记录失败: " + e.getMessage());
            return false;
        }
    }

    // 统计指定用户的总支付金额
    public int getTotalPaymentByUserId(String userId) {
        String sql = "SELECT SUM(p.payment) FROM payrecord p " +
                "INNER JOIN `order` o ON p.order_ID = o.order_ID " +
                "WHERE o.user_ID = ? AND p.pay_state = '已支付'";
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
            System.err.println("❌ 统计用户总支付金额失败: " + e.getMessage());
            return 0;
        }
    }

    // 统计指定时间范围内的总收入
    public int getTotalIncomeByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT SUM(payment) FROM payrecord " +
                "WHERE pay_state = '已支付' AND pay_time BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startTime));
            stmt.setTimestamp(2, Timestamp.valueOf(endTime));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ 统计时间范围收入失败: " + e.getMessage());
            return 0;
        }
    }

    // 统计各支付方式的使用次数
    public List<PayMethodStats> getPayMethodStatistics() {
        String sql = "SELECT pay_method, COUNT(*) as count, SUM(payment) as total_amount " +
                "FROM payrecord WHERE pay_state = '已支付' " +
                "GROUP BY pay_method ORDER BY count DESC";
        List<PayMethodStats> stats = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                PayMethodStats stat = new PayMethodStats();
                stat.payMethod = rs.getString("pay_method");
                stat.count = rs.getInt("count");
                stat.totalAmount = rs.getInt("total_amount");
                stats.add(stat);
            }
            return stats;
        } catch (SQLException e) {
            System.err.println("❌ 统计支付方式失败: " + e.getMessage());
            throw new RuntimeException("统计支付方式失败", e);
        }
    }

    // 删除支付记录（谨慎使用，一般只用于测试）
    public boolean deletePayrecord(String payId) {
        String sql = "DELETE FROM payrecord WHERE pay_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, payId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ 支付记录删除成功: " + payId);
                return true;
            }
            System.out.println("⚠️ 未找到要删除的支付记录: " + payId);
            return false;
        } catch (SQLException e) {
            System.err.println("❌ 删除支付记录失败: " + e.getMessage());
            throw new RuntimeException("删除支付记录失败", e);
        }
    }

    // 批量更新支付状态（管理员功能）
    public int batchUpdatePayState(List<String> payIds, String newState) {
        if (payIds == null || payIds.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE payrecord SET pay_state = ?, pay_time = ? WHERE pay_ID = ?";
        int successCount = 0;
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // 开启事务

            for (String payId : payIds) {
                stmt.setString(1, newState);
                stmt.setTimestamp(2, Timestamp.valueOf(now));
                stmt.setString(3, payId);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit(); // 提交事务

            for (int result : results) {
                if (result > 0) successCount++;
            }

            System.out.println("✅ 批量更新支付状态完成: " + successCount + "/" + payIds.size() + " 成功");
            return successCount;

        } catch (SQLException e) {
            System.err.println("❌ 批量更新支付状态失败: " + e.getMessage());
            throw new RuntimeException("批量更新支付状态失败", e);
        }
    }

    // 私有辅助方法：将ResultSet映射为Payrecord对象
    private Payrecord mapResultSetToPayrecord(ResultSet rs) throws SQLException {
        Payrecord payrecord = new Payrecord();
        payrecord.setPayId(rs.getString("pay_ID"));
        payrecord.setOrderId(rs.getString("order_ID"));
        payrecord.setPayment(rs.getInt("payment"));
        payrecord.setPayMethod(rs.getString("pay_method"));
        payrecord.setPayState(rs.getString("pay_state"));
        payrecord.setPayTime(rs.getTimestamp("pay_time").toLocalDateTime());
        return payrecord;
    }

    // 支付方式统计内部类
    public static class PayMethodStats {
        public String payMethod;
        public int count;
        public int totalAmount;

        @Override
        public String toString() {
            return "PayMethodStats{" +
                    "payMethod='" + payMethod + '\'' +
                    ", count=" + count +
                    ", totalAmount=" + totalAmount +
                    '}';
        }
    }
}