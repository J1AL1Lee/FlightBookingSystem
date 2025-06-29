package dao;

import model.Flightrecord;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FlightrecordDao {

    /**
     * 保存航程记录
     * @param flightrecord 航程对象
     * @return 保存成功的航程ID
     */
    public String save(Flightrecord flightrecord) {
        String sql = "INSERT INTO flightrecord (flightrecord_ID, flight_ID, flight_date, seat0_left, seat1_left) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightrecord.getFlightrecordId());
            ps.setString(2, flightrecord.getFlightId());
            ps.setDate(3, Date.valueOf(flightrecord.getFlightDate()));
            ps.setInt(4, flightrecord.getSeat0Left());
            ps.setInt(5, flightrecord.getSeat1Left());

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ 航程记录保存成功: " + flightrecord.getFlightrecordId());
                return flightrecord.getFlightrecordId();
            } else {
                throw new RuntimeException("保存航程记录失败");
            }

        } catch (SQLException e) {
            System.err.println("❌ 保存航程记录失败: " + e.getMessage());
            throw new RuntimeException("保存航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据航程ID查找航程记录
     * @param flightrecordId 航程ID
     * @return 航程记录对象，如果不存在返回null
     */
    public Flightrecord findById(String flightrecordId) {
        String sql = "SELECT * FROM flightrecord WHERE flightrecord_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightrecordId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToFlightrecord(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 查询航程记录失败: " + e.getMessage());
            throw new RuntimeException("查询航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 核心查询：根据航班号和日期查找航程记录
     * @param flightId 航班号
     * @param flightDate 航班日期
     * @return 航程记录对象，如果不存在返回null
     */
    public Flightrecord findByFlightAndDate(String flightId, LocalDate flightDate) {
        String sql = "SELECT * FROM flightrecord WHERE flight_ID = ? AND flight_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            ps.setDate(2, Date.valueOf(flightDate));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToFlightrecord(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 根据航班和日期查询航程记录失败: " + e.getMessage());
            throw new RuntimeException("根据航班和日期查询航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询某个航班在指定日期范围内的所有航程记录
     * @param flightId 航班号
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 航程记录列表
     */
    public List<Flightrecord> findByFlightAndDateRange(String flightId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM flightrecord WHERE flight_ID = ? AND flight_date BETWEEN ? AND ? ORDER BY flight_date";
        List<Flightrecord> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(mapResultSetToFlightrecord(rs));
            }

            System.out.println("📅 查询航班 " + flightId + " 在 " + startDate + " 至 " + endDate + " 的航程记录: " + records.size() + " 条");
            return records;

        } catch (SQLException e) {
            System.err.println("❌ 根据航班和日期范围查询航程记录失败: " + e.getMessage());
            throw new RuntimeException("根据航班和日期范围查询航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定日期的所有航程记录
     * @param flightDate 航班日期
     * @return 航程记录列表
     */
    public List<Flightrecord> findByDate(LocalDate flightDate) {
        String sql = "SELECT * FROM flightrecord WHERE flight_date = ? ORDER BY flight_ID";
        List<Flightrecord> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(flightDate));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(mapResultSetToFlightrecord(rs));
            }

            System.out.println("📅 查询 " + flightDate + " 的航程记录: " + records.size() + " 条");
            return records;

        } catch (SQLException e) {
            System.err.println("❌ 根据日期查询航程记录失败: " + e.getMessage());
            throw new RuntimeException("根据日期查询航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询有可用座位的航程记录
     * @param flightDate 航班日期
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param requiredSeats 需要的座位数
     * @return 有足够座位的航程记录列表
     */
    public List<Flightrecord> findAvailableFlightrecords(LocalDate flightDate, int seatType, int requiredSeats) {
        String seatColumn = (seatType == 0) ? "seat0_left" : "seat1_left";
        String sql = "SELECT * FROM flightrecord WHERE flight_date = ? AND " + seatColumn + " >= ? ORDER BY flight_ID";
        List<Flightrecord> records = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(flightDate));
            ps.setInt(2, requiredSeats);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(mapResultSetToFlightrecord(rs));
            }

            String seatTypeName = (seatType == 0) ? "经济舱" : "商务舱";
            System.out.println("🪑 查询 " + flightDate + " 有 " + requiredSeats + " 个" + seatTypeName + "座位的航程: " + records.size() + " 条");
            return records;

        } catch (SQLException e) {
            System.err.println("❌ 查询可用座位航程失败: " + e.getMessage());
            throw new RuntimeException("查询可用座位航程失败: " + e.getMessage());
        }
    }

    /**
     * 更新剩余座位数（预订座位时使用）
     * @param flightrecordId 航程ID
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param seatCount 座位数量（正数为增加，负数为减少）
     * @return 是否更新成功
     */
    public boolean updateSeatCount(String flightrecordId, int seatType, int seatCount) {
        String seatColumn = (seatType == 0) ? "seat0_left" : "seat1_left";
        String sql = "UPDATE flightrecord SET " + seatColumn + " = " + seatColumn + " + ? WHERE flightrecord_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, seatCount);
            ps.setString(2, flightrecordId);

            int result = ps.executeUpdate();
            if (result > 0) {
                String operation = seatCount > 0 ? "增加" : "减少";
                String seatTypeName = (seatType == 0) ? "经济舱" : "商务舱";
                System.out.println("✅ 座位数更新成功: " + flightrecordId + " " + operation + " " + Math.abs(seatCount) + " 个" + seatTypeName + "座位");
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 更新座位数失败: " + e.getMessage());
            throw new RuntimeException("更新座位数失败: " + e.getMessage());
        }
    }

    /**
     * 预订座位（减少剩余座位数）
     * @param flightrecordId 航程ID
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param seatCount 预订的座位数
     * @return 是否预订成功
     */
    public boolean bookSeats(String flightrecordId, int seatType, int seatCount) {
        // 先检查是否有足够座位
        Flightrecord record = findById(flightrecordId);
        if (record == null) {
            System.err.println("❌ 航程记录不存在: " + flightrecordId);
            return false;
        }

        int availableSeats = (seatType == 0) ? record.getSeat0Left() : record.getSeat1Left();
        if (availableSeats < seatCount) {
            String seatTypeName = (seatType == 0) ? "经济舱" : "商务舱";
            System.err.println("❌ 座位不足: 需要 " + seatCount + " 个" + seatTypeName + "座位，剩余 " + availableSeats + " 个");
            return false;
        }

        // 减少座位数
        return updateSeatCount(flightrecordId, seatType, -seatCount);
    }

    /**
     * 取消预订（增加剩余座位数）
     * @param flightrecordId 航程ID
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param seatCount 取消的座位数
     * @return 是否取消成功
     */
    public boolean cancelSeats(String flightrecordId, int seatType, int seatCount) {
        return updateSeatCount(flightrecordId, seatType, seatCount);
    }

    /**
     * 更新航程记录
     * @param flightrecord 航程对象
     * @return 是否更新成功
     */
    public boolean update(Flightrecord flightrecord) {
        String sql = "UPDATE flightrecord SET flight_ID = ?, flight_date = ?, seat0_left = ?, seat1_left = ? " +
                "WHERE flightrecord_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightrecord.getFlightId());
            ps.setDate(2, Date.valueOf(flightrecord.getFlightDate()));
            ps.setInt(3, flightrecord.getSeat0Left());
            ps.setInt(4, flightrecord.getSeat1Left());
            ps.setString(5, flightrecord.getFlightrecordId());

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ 航程记录更新成功: " + flightrecord.getFlightrecordId());
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 更新航程记录失败: " + e.getMessage());
            throw new RuntimeException("更新航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除航程记录
     * @param flightrecordId 航程ID
     * @return 是否删除成功
     */
    public boolean deleteById(String flightrecordId) {
        String sql = "DELETE FROM flightrecord WHERE flightrecord_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightrecordId);
            int result = ps.executeUpdate();

            if (result > 0) {
                System.out.println("✅ 航程记录删除成功: " + flightrecordId);
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 删除航程记录失败: " + e.getMessage());
            throw new RuntimeException("删除航程记录失败: " + e.getMessage());
        }
    }

    /**
     * 检查航程记录是否存在
     * @param flightrecordId 航程ID
     * @return 是否存在
     */
    public boolean existsById(String flightrecordId) {
        String sql = "SELECT COUNT(*) FROM flightrecord WHERE flightrecord_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightrecordId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 检查航程记录是否存在失败: " + e.getMessage());
            throw new RuntimeException("检查航程记录是否存在失败: " + e.getMessage());
        }
    }

    /**
     * 检查某航班在指定日期是否有航程记录
     * @param flightId 航班号
     * @param flightDate 航班日期
     * @return 是否存在
     */
    public boolean existsByFlightAndDate(String flightId, LocalDate flightDate) {
        String sql = "SELECT COUNT(*) FROM flightrecord WHERE flight_ID = ? AND flight_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            ps.setDate(2, Date.valueOf(flightDate));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 检查航班日期组合是否存在失败: " + e.getMessage());
            throw new RuntimeException("检查航班日期组合是否存在失败: " + e.getMessage());
        }
    }

    /**
     * 统计某日期的航程记录总数
     * @param flightDate 航班日期
     * @return 航程记录总数
     */
    public int countByDate(LocalDate flightDate) {
        String sql = "SELECT COUNT(*) FROM flightrecord WHERE flight_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(flightDate));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("❌ 统计日期航程记录数失败: " + e.getMessage());
            throw new RuntimeException("统计日期航程记录数失败: " + e.getMessage());
        }
    }

    /**
     * 生成航程记录ID
     * @param flightId 航班号
     * @param flightDate 航班日期
     * @return 航程记录ID (格式: 航班号+日期，如CA123420250701)
     */
    public static String generateFlightrecordId(String flightId, LocalDate flightDate) {
        String dateStr = flightDate.toString().replace("-", "");
        return flightId + dateStr;
    }

    /**
     * 根据航班创建航程记录（初始化座位数）
     * @param flightId 航班号
     * @param flightDate 航班日期
     * @param seat0Capacity 经济舱总座位数
     * @param seat1Capacity 商务舱总座位数
     * @return 创建成功的航程记录ID
     */
    public String createFlightrecordFromFlight(String flightId, LocalDate flightDate,
                                               int seat0Capacity, int seat1Capacity) {
        String flightrecordId = generateFlightrecordId(flightId, flightDate);

        // 检查是否已存在
        if (existsById(flightrecordId)) {
            System.err.println("❌ 航程记录已存在: " + flightrecordId);
            return null;
        }

        Flightrecord record = new Flightrecord();
        record.setFlightrecordId(flightrecordId);
        record.setFlightId(flightId);
        record.setFlightDate(flightDate);
        record.setSeat0Left(seat0Capacity);  // 初始剩余座位 = 总座位数
        record.setSeat1Left(seat1Capacity);

        return save(record);
    }

    /**
     * 批量创建航程记录
     * @param flightrecords 航程记录列表
     * @return 成功保存的数量
     */
    public int batchSave(List<Flightrecord> flightrecords) {
        String sql = "INSERT INTO flightrecord (flightrecord_ID, flight_ID, flight_date, seat0_left, seat1_left) " +
                "VALUES (?, ?, ?, ?, ?)";
        int savedCount = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Flightrecord record : flightrecords) {
                    ps.setString(1, record.getFlightrecordId());
                    ps.setString(2, record.getFlightId());
                    ps.setDate(3, Date.valueOf(record.getFlightDate()));
                    ps.setInt(4, record.getSeat0Left());
                    ps.setInt(5, record.getSeat1Left());
                    ps.addBatch();
                }

                int[] results = ps.executeBatch();
                conn.commit(); // 提交事务

                for (int result : results) {
                    if (result > 0) savedCount++;
                }

                System.out.println("✅ 批量保存航程记录成功: " + savedCount + "/" + flightrecords.size());

            } catch (SQLException e) {
                conn.rollback(); // 回滚事务
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("❌ 批量保存航程记录失败: " + e.getMessage());
            throw new RuntimeException("批量保存航程记录失败: " + e.getMessage());
        }

        return savedCount;
    }

    /**
     * 将ResultSet映射为Flightrecord对象
     * @param rs ResultSet对象
     * @return Flightrecord对象
     * @throws SQLException SQL异常
     */
    private Flightrecord mapResultSetToFlightrecord(ResultSet rs) throws SQLException {
        Flightrecord record = new Flightrecord();
        record.setFlightrecordId(rs.getString("flightrecord_ID"));
        record.setFlightId(rs.getString("flight_ID"));
        record.setFlightDate(rs.getDate("flight_date").toLocalDate());
        record.setSeat0Left(rs.getInt("seat0_left"));
        record.setSeat1Left(rs.getInt("seat1_left"));
        return record;
    }
}