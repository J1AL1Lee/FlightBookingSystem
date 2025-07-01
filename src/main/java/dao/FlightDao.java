package dao;

import model.Flight;
import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FlightDao {

    /**
     * 保存航班信息
     * @param flight 航班对象
     * @return 保存成功的航班ID
     */
    public String save(Flight flight) {
        String sql = "INSERT INTO flight (flight_ID, airlinecompany_ID, airport_from, airport_to, " +
                "time_takeoff, time_arrive, seat0_capacity, seat1_capacity, seat0_price, seat1_price, discount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flight.getFlightId());
            ps.setString(2, flight.getAirlinecompanyId());
            ps.setString(3, flight.getAirportFrom());
            ps.setString(4, flight.getAirportTo());
            ps.setTime(5, Time.valueOf(flight.getTimeTakeoff()));
            ps.setTime(6, Time.valueOf(flight.getTimeArrive()));
            ps.setInt(7, flight.getSeat0Capacity());
            ps.setInt(8, flight.getSeat1Capacity());
            ps.setInt(9, flight.getSeat0Price());
            ps.setInt(10, flight.getSeat1Price());
            ps.setFloat(11, flight.getDiscount());

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ 航班保存成功: " + flight.getFlightId());
                return flight.getFlightId();
            } else {
                throw new RuntimeException("保存航班失败");
            }

        } catch (SQLException e) {
            System.err.println("❌ 保存航班失败: " + e.getMessage());
            throw new RuntimeException("保存航班失败: " + e.getMessage());
        }
    }

    /**
     * 根据航班号查找航班
     * @param flightId 航班号
     * @return 航班对象，如果不存在返回null
     */
    public Flight findById(String flightId) {
        String sql = "SELECT * FROM flight WHERE flight_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToFlight(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 查询航班失败: " + e.getMessage());
            throw new RuntimeException("查询航班失败: " + e.getMessage());
        }
    }

    /**
     * 查找所有航班
     * @return 航班列表
     */
    public List<Flight> findAll() {
        String sql = "SELECT * FROM flight ORDER BY flight_ID";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("📊 查询到 " + flights.size() + " 个航班");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 查询所有航班失败: " + e.getMessage());
            throw new RuntimeException("查询所有航班失败: " + e.getMessage());
        }
    }

    /**
     * 模糊查询航班信息
     * @param airportFrom 出发地关键词（支持模糊查询，如"上海"、"北京"等）
     * @param airportTo 目的地关键词（支持模糊查询，如"上海"、"北京"等）
     * @return 匹配的航班列表，按出发时间排序
     */
    public List<Flight> searchFlights(String airportFrom, String airportTo) {
        // 使用 LIKE 进行模糊查询，% 通配符匹配任意字符
        String sql = "SELECT * FROM flight " +
                "WHERE (airport_from LIKE ? OR airport_from LIKE ?) " +
                "AND (airport_to LIKE ? OR airport_to LIKE ?) " +
                "ORDER BY time_takeoff";

        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 设置模糊查询参数
            // 对于出发地：既匹配包含关键词的，也匹配以关键词开头的
            ps.setString(1, "%" + airportFrom + "%");  // 包含关键词
            ps.setString(2, airportFrom + "%");        // 以关键词开头

            // 对于目的地：同样处理
            ps.setString(3, "%" + airportTo + "%");    // 包含关键词
            ps.setString(4, airportTo + "%");          // 以关键词开头

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("🔍 模糊搜索航班 \"" + airportFrom + "\" → \"" + airportTo +
                    "\"，找到 " + flights.size() + " 个航班");

            // 打印匹配的机场信息，方便调试
            if (!flights.isEmpty()) {
                Set<String> fromAirports = flights.stream()
                        .map(Flight::getAirportFrom)
                        .collect(Collectors.toSet());
                Set<String> toAirports = flights.stream()
                        .map(Flight::getAirportTo)
                        .collect(Collectors.toSet());

                System.out.println("📍 匹配的出发机场: " + String.join(", ", fromAirports));
                System.out.println("📍 匹配的到达机场: " + String.join(", ", toAirports));
            }

            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 模糊搜索航班失败: " + e.getMessage());
            throw new RuntimeException("搜索航班失败: " + e.getMessage());
        }
    }


    /**
     * 根据航空公司查找航班
     * @param airlinecompanyId 航空公司ID
     * @return 该航空公司的航班列表
     */
    public List<Flight> findByAirline(String airlinecompanyId) {
        String sql = "SELECT * FROM flight WHERE airlinecompany_ID = ? ORDER BY time_takeoff";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompanyId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("🏢 查询航空公司 " + airlinecompanyId + " 的航班，找到 " + flights.size() + " 个");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 根据航空公司查询航班失败: " + e.getMessage());
            throw new RuntimeException("根据航空公司查询航班失败: " + e.getMessage());
        }
    }

    /**
     * 根据时间范围搜索航班
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间范围内的航班列表
     */
    public List<Flight> searchFlightsByTimeRange(String airportFrom, String airportTo,
                                                 LocalTime startTime, LocalTime endTime) {
        String sql = "SELECT * FROM flight WHERE airport_from = ? AND airport_to = ? " +
                "AND time_takeoff BETWEEN ? AND ? ORDER BY time_takeoff";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airportFrom);
            ps.setString(2, airportTo);
            ps.setTime(3, Time.valueOf(startTime));
            ps.setTime(4, Time.valueOf(endTime));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("⏰ 搜索 " + startTime + "-" + endTime + " 时间段的航班，找到 " + flights.size() + " 个");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 根据时间范围搜索航班失败: " + e.getMessage());
            throw new RuntimeException("根据时间范围搜索航班失败: " + e.getMessage());
        }
    }

    /**
     * 根据价格范围搜索航班（经济舱）
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 价格范围内的航班列表
     */
    public List<Flight> searchFlightsByPriceRange(String airportFrom, String airportTo,
                                                  int minPrice, int maxPrice) {
        String sql = "SELECT * FROM flight WHERE airport_from = ? AND airport_to = ? " +
                "AND (seat0_price * discount) BETWEEN ? AND ? ORDER BY (seat0_price * discount)";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airportFrom);
            ps.setString(2, airportTo);
            ps.setInt(3, minPrice);
            ps.setInt(4, maxPrice);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("💰 搜索价格 " + minPrice + "-" + maxPrice + " 元的航班，找到 " + flights.size() + " 个");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 根据价格范围搜索航班失败: " + e.getMessage());
            throw new RuntimeException("根据价格范围搜索航班失败: " + e.getMessage());
        }
    }

    /**
     * 查找最便宜的航班
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @return 最便宜的航班
     */
    public Flight findCheapestFlight(String airportFrom, String airportTo) {
        String sql = "SELECT * FROM flight WHERE airport_from = ? AND airport_to = ? " +
                "ORDER BY (seat0_price * discount) ASC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airportFrom);
            ps.setString(2, airportTo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Flight cheapest = mapResultSetToFlight(rs);
                System.out.println("💰 最便宜航班: " + cheapest.getFlightId() +
                        " 价格: " + (cheapest.getSeat0Price() * cheapest.getDiscount()));
                return cheapest;
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 查找最便宜航班失败: " + e.getMessage());
            throw new RuntimeException("查找最便宜航班失败: " + e.getMessage());
        }
    }

    /**
     * 查找有足够座位的航班
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param requiredSeats 需要的座位数
     * @param seatClass 座位等级 (0-经济舱, 1-商务舱)
     * @return 有足够座位的航班列表
     */
    public List<Flight> findAvailableFlights(String airportFrom, String airportTo,
                                             int requiredSeats, int seatClass) {
        String capacityColumn = (seatClass == 0) ? "seat0_capacity" : "seat1_capacity";
        String sql = "SELECT * FROM flight WHERE airport_from = ? AND airport_to = ? " +
                "AND " + capacityColumn + " >= ? ORDER BY time_takeoff";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airportFrom);
            ps.setString(2, airportTo);
            ps.setInt(3, requiredSeats);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            String classType = (seatClass == 0) ? "经济舱" : "商务舱";
            System.out.println("🪑 搜索有 " + requiredSeats + " 个" + classType + "座位的航班，找到 " + flights.size() + " 个");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 查找可用座位航班失败: " + e.getMessage());
            throw new RuntimeException("查找可用座位航班失败: " + e.getMessage());
        }
    }

    /**
     * 综合搜索航班（支持多条件）
     * @param searchParams 搜索参数对象
     * @return 匹配的航班列表
     */
    public List<Flight> searchFlightsAdvanced(FlightSearchParams searchParams) {
        StringBuilder sql = new StringBuilder("SELECT * FROM flight WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 动态构建SQL和参数
        if (searchParams.getAirportFrom() != null) {
            sql.append(" AND airport_from = ?");
            params.add(searchParams.getAirportFrom());
        }

        if (searchParams.getAirportTo() != null) {
            sql.append(" AND airport_to = ?");
            params.add(searchParams.getAirportTo());
        }

        if (searchParams.getAirlinecompanyId() != null) {
            sql.append(" AND airlinecompany_ID = ?");
            params.add(searchParams.getAirlinecompanyId());
        }

        if (searchParams.getMinPrice() != null && searchParams.getMaxPrice() != null) {
            sql.append(" AND (seat0_price * discount) BETWEEN ? AND ?");
            params.add(searchParams.getMinPrice());
            params.add(searchParams.getMaxPrice());
        }

        if (searchParams.getStartTime() != null && searchParams.getEndTime() != null) {
            sql.append(" AND time_takeoff BETWEEN ? AND ?");
            params.add(Time.valueOf(searchParams.getStartTime()));
            params.add(Time.valueOf(searchParams.getEndTime()));
        }

        sql.append(" ORDER BY time_takeoff");

        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("🔍 高级搜索找到 " + flights.size() + " 个航班");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 高级搜索失败: " + e.getMessage());
            throw new RuntimeException("高级搜索失败: " + e.getMessage());
        }
    }

    // 在 FlightDao 中添加以下方法

    /**
     * 根据航班号模式模糊查询航班
     * @param flightIdPattern 航班号模式（支持LIKE查询）
     * @return 匹配的航班列表
     */
    public List<Flight> findByIdPattern(String flightIdPattern) {
        String sql = "SELECT * FROM flight WHERE flight_ID LIKE ? ORDER BY flight_ID";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 添加通配符支持模糊查询
            ps.setString(1, "%" + flightIdPattern + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("🔍 模糊查询航班号包含 \"" + flightIdPattern + "\" 找到 " + flights.size() + " 个航班");

            // 打印匹配的航班号，方便调试
            if (!flights.isEmpty()) {
                List<String> flightIds = flights.stream()
                        .map(Flight::getFlightId)
                        .collect(Collectors.toList());
                System.out.println("📍 匹配的航班号: " + String.join(", ", flightIds));
            }

            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 模糊查询航班号失败: " + e.getMessage());
            throw new RuntimeException("模糊查询航班号失败: " + e.getMessage());
        }
    }

    /**
     * 根据航班号前缀查询航班（常用于航空公司代码搜索）
     * @param prefix 航班号前缀，如"CZ"、"CA"等
     * @return 匹配的航班列表
     */
    public List<Flight> findByIdPrefix(String prefix) {
        String sql = "SELECT * FROM flight WHERE flight_ID LIKE ? ORDER BY flight_ID";
        List<Flight> flights = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 前缀匹配
            ps.setString(1, prefix + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                flights.add(mapResultSetToFlight(rs));
            }

            System.out.println("🔍 查询航班号前缀为 \"" + prefix + "\" 的航班，找到 " + flights.size() + " 个");
            return flights;

        } catch (SQLException e) {
            System.err.println("❌ 根据前缀查询航班失败: " + e.getMessage());
            throw new RuntimeException("根据前缀查询航班失败: " + e.getMessage());
        }
    }

    /**
     * 智能搜索航班号（支持多种模式）
     * @param searchTerm 搜索词
     * @return 匹配的航班列表
     */
    public List<Flight> smartSearchFlightId(String searchTerm) {
        List<Flight> results = new ArrayList<>();

        // 1. 先尝试精确匹配
        Flight exactMatch = findById(searchTerm.toUpperCase());
        if (exactMatch != null) {
            results.add(exactMatch);
            System.out.println("✅ 精确匹配航班: " + searchTerm);
            return results;
        }

        // 2. 再尝试前缀匹配（如果搜索词是2-3位，可能是航空公司代码）
        if (searchTerm.length() <= 3) {
            results = findByIdPrefix(searchTerm.toUpperCase());
            if (!results.isEmpty()) {
                System.out.println("✅ 前缀匹配航班: " + searchTerm);
                return results;
            }
        }

        // 3. 最后尝试模糊匹配
        results = findByIdPattern(searchTerm.toUpperCase());
        System.out.println("✅ 模糊匹配航班: " + searchTerm);

        return results;
    }

    /**
     * 更新航班信息
     * @param flight 航班对象
     * @return 是否更新成功
     */
    public boolean update(Flight flight) {
        String sql = "UPDATE flight SET airlinecompany_ID = ?, airport_from = ?, airport_to = ?, " +
                "time_takeoff = ?, time_arrive = ?, seat0_capacity = ?, seat1_capacity = ?, " +
                "seat0_price = ?, seat1_price = ?, discount = ? WHERE flight_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flight.getAirlinecompanyId());
            ps.setString(2, flight.getAirportFrom());
            ps.setString(3, flight.getAirportTo());
            ps.setTime(4, Time.valueOf(flight.getTimeTakeoff()));
            ps.setTime(5, Time.valueOf(flight.getTimeArrive()));
            ps.setInt(6, flight.getSeat0Capacity());
            ps.setInt(7, flight.getSeat1Capacity());
            ps.setInt(8, flight.getSeat0Price());
            ps.setInt(9, flight.getSeat1Price());
            ps.setFloat(10, flight.getDiscount());
            ps.setString(11, flight.getFlightId());

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ 航班更新成功: " + flight.getFlightId());
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 更新航班失败: " + e.getMessage());
            throw new RuntimeException("更新航班失败: " + e.getMessage());
        }
    }

    /**
     * 删除航班
     * @param flightId 航班号
     * @return 是否删除成功
     */
    public boolean deleteById(String flightId) {
        String sql = "DELETE FROM flight WHERE flight_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            int result = ps.executeUpdate();

            if (result > 0) {
                System.out.println("✅ 航班删除成功: " + flightId);
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 删除航班失败: " + e.getMessage());
            throw new RuntimeException("删除航班失败: " + e.getMessage());
        }
    }

    /**
     * 检查航班是否存在
     * @param flightId 航班号
     * @return 是否存在
     */
    public boolean existsById(String flightId) {
        String sql = "SELECT COUNT(*) FROM flight WHERE flight_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 检查航班是否存在失败: " + e.getMessage());
            throw new RuntimeException("检查航班是否存在失败: " + e.getMessage());
        }
    }

    /**
     * 统计航班总数
     * @return 航班总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM flight";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("❌ 统计航班数量失败: " + e.getMessage());
            throw new RuntimeException("统计航班数量失败: " + e.getMessage());
        }
    }

    /**
     * 统计某条航线的航班数
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @return 该航线的航班数
     */
    public int countByRoute(String airportFrom, String airportTo) {
        String sql = "SELECT COUNT(*) FROM flight WHERE airport_from = ? AND airport_to = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airportFrom);
            ps.setString(2, airportTo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("❌ 统计航线航班数失败: " + e.getMessage());
            throw new RuntimeException("统计航线航班数失败: " + e.getMessage());
        }
    }

    /**
     * 查找热门航线
     * @param limit 返回数量限制
     * @return 热门航线列表（起降机场对）
     */
    public List<String> findPopularRoutes(int limit) {
        String sql = "SELECT CONCAT(airport_from, '-', airport_to) as route, COUNT(*) as flight_count " +
                "FROM flight GROUP BY airport_from, airport_to ORDER BY flight_count DESC LIMIT ?";
        List<String> routes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                routes.add(rs.getString("route") + " (" + rs.getInt("flight_count") + "航班)");
            }

            System.out.println("🔥 查询到 " + routes.size() + " 条热门航线");
            return routes;

        } catch (SQLException e) {
            System.err.println("❌ 查询热门航线失败: " + e.getMessage());
            throw new RuntimeException("查询热门航线失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存航班
     * @param flights 航班列表
     * @return 成功保存的数量
     */
    public int batchSave(List<Flight> flights) {
        String sql = "INSERT INTO flight (flight_ID, airlinecompany_ID, airport_from, airport_to, " +
                "time_takeoff, time_arrive, seat0_capacity, seat1_capacity, seat0_price, seat1_price, discount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int savedCount = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Flight flight : flights) {
                    ps.setString(1, flight.getFlightId());
                    ps.setString(2, flight.getAirlinecompanyId());
                    ps.setString(3, flight.getAirportFrom());
                    ps.setString(4, flight.getAirportTo());
                    ps.setTime(5, Time.valueOf(flight.getTimeTakeoff()));
                    ps.setTime(6, Time.valueOf(flight.getTimeArrive()));
                    ps.setInt(7, flight.getSeat0Capacity());
                    ps.setInt(8, flight.getSeat1Capacity());
                    ps.setInt(9, flight.getSeat0Price());
                    ps.setInt(10, flight.getSeat1Price());
                    ps.setFloat(11, flight.getDiscount());
                    ps.addBatch();
                }

                int[] results = ps.executeBatch();
                conn.commit(); // 提交事务

                for (int result : results) {
                    if (result > 0) savedCount++;
                }

                System.out.println("✅ 批量保存航班成功: " + savedCount + "/" + flights.size());

            } catch (SQLException e) {
                conn.rollback(); // 回滚事务
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("❌ 批量保存航班失败: " + e.getMessage());
            throw new RuntimeException("批量保存航班失败: " + e.getMessage());
        }

        return savedCount;
    }

    /**
     * 将ResultSet映射为Flight对象
     * @param rs ResultSet对象
     * @return Flight对象
     * @throws SQLException SQL异常
     */
    private Flight mapResultSetToFlight(ResultSet rs) throws SQLException {
        Flight flight = new Flight();
        flight.setFlightId(rs.getString("flight_ID"));
        flight.setAirlinecompanyId(rs.getString("airlinecompany_ID"));
        flight.setAirportFrom(rs.getString("airport_from"));
        flight.setAirportTo(rs.getString("airport_to"));
        flight.setTimeTakeoff(rs.getTime("time_takeoff").toLocalTime());
        flight.setTimeArrive(rs.getTime("time_arrive").toLocalTime());
        flight.setSeat0Capacity(rs.getInt("seat0_capacity"));
        flight.setSeat1Capacity(rs.getInt("seat1_capacity"));
        flight.setSeat0Price(rs.getInt("seat0_price"));
        flight.setSeat1Price(rs.getInt("seat1_price"));
        flight.setDiscount(rs.getFloat("discount"));
        return flight;
    }

    /**
     * 航班搜索参数类
     */
    public static class FlightSearchParams {
        private String airportFrom;
        private String airportTo;
        private String airlinecompanyId;
        private Integer minPrice;
        private Integer maxPrice;
        private LocalTime startTime;
        private LocalTime endTime;

        // 构造函数和Getter/Setter
        public FlightSearchParams() {}

        public String getAirportFrom() { return airportFrom; }
        public void setAirportFrom(String airportFrom) { this.airportFrom = airportFrom; }
        public String getAirportTo() { return airportTo; }
        public void setAirportTo(String airportTo) { this.airportTo = airportTo; }
        public String getAirlinecompanyId() { return airlinecompanyId; }
        public void setAirlinecompanyId(String airlinecompanyId) { this.airlinecompanyId = airlinecompanyId; }
        public Integer getMinPrice() { return minPrice; }
        public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }
        public Integer getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Integer maxPrice) { this.maxPrice = maxPrice; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    }
}