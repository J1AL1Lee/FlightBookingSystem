
package dao;

import model.Airlinecompany;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AirlinecompanyDao {

    /**
     * 保存航空公司信息
     * @param airlinecompany 航空公司对象
     * @return 保存成功的航空公司ID
     */
    public String save(Airlinecompany airlinecompany) {
        String sql = "INSERT INTO airlinecompany (airlinecompany_ID, airlinecompany_name, airlinecompany_telephone) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompany.getAirlinecompanyId());
            ps.setString(2, airlinecompany.getAirlinecompanyName());
            ps.setString(3, airlinecompany.getAirlinecompanyTelephone());

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ 航空公司保存成功: " + airlinecompany.getAirlinecompanyName());
                return airlinecompany.getAirlinecompanyId();
            } else {
                throw new RuntimeException("保存航空公司失败");
            }

        } catch (SQLException e) {
            System.err.println("❌ 保存航空公司失败: " + e.getMessage());
            throw new RuntimeException("保存航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查找航空公司
     * @param airlinecompanyId 航空公司ID
     * @return 航空公司对象，如果不存在返回null
     */
    public Airlinecompany findById(String airlinecompanyId) {
        String sql = "SELECT * FROM airlinecompany WHERE airlinecompany_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompanyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToAirlinecompany(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 查询航空公司失败: " + e.getMessage());
            throw new RuntimeException("查询航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 根据航空公司名称查找
     * @param airlinecompanyName 航空公司名称
     * @return 航空公司对象，如果不存在返回null
     */
    public Airlinecompany findByName(String airlinecompanyName) {
        String sql = "SELECT * FROM airlinecompany WHERE airlinecompany_name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompanyName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToAirlinecompany(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ 根据名称查询航空公司失败: " + e.getMessage());
            throw new RuntimeException("根据名称查询航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 查找所有航空公司
     * @return 航空公司列表
     */
    public List<Airlinecompany> findAll() {
        String sql = "SELECT * FROM airlinecompany ORDER BY airlinecompany_ID";
        List<Airlinecompany> airlinecompanies = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                airlinecompanies.add(mapResultSetToAirlinecompany(rs));
            }

            System.out.println("📊 查询到 " + airlinecompanies.size() + " 家航空公司");
            return airlinecompanies;

        } catch (SQLException e) {
            System.err.println("❌ 查询所有航空公司失败: " + e.getMessage());
            throw new RuntimeException("查询所有航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 模糊查询航空公司（根据名称）
     * @param keyword 关键词
     * @return 匹配的航空公司列表
     */
    public List<Airlinecompany> findByNameLike(String keyword) {
        String sql = "SELECT * FROM airlinecompany WHERE airlinecompany_name LIKE ? ORDER BY airlinecompany_ID";
        List<Airlinecompany> airlinecompanies = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                airlinecompanies.add(mapResultSetToAirlinecompany(rs));
            }

            System.out.println("🔍 根据关键词 '" + keyword + "' 查询到 " + airlinecompanies.size() + " 家航空公司");
            return airlinecompanies;

        } catch (SQLException e) {
            System.err.println("❌ 模糊查询航空公司失败: " + e.getMessage());
            throw new RuntimeException("模糊查询航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 更新航空公司信息
     * @param airlinecompany 航空公司对象
     * @return 是否更新成功
     */
    public boolean update(Airlinecompany airlinecompany) {
        String sql = "UPDATE airlinecompany SET airlinecompany_name = ?, airlinecompany_telephone = ? WHERE airlinecompany_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompany.getAirlinecompanyName());
            ps.setString(2, airlinecompany.getAirlinecompanyTelephone());
            ps.setString(3, airlinecompany.getAirlinecompanyId());

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ 航空公司更新成功: " + airlinecompany.getAirlinecompanyName());
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 更新航空公司失败: " + e.getMessage());
            throw new RuntimeException("更新航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 删除航空公司
     * @param airlinecompanyId 航空公司ID
     * @return 是否删除成功
     */
    public boolean deleteById(String airlinecompanyId) {
        String sql = "DELETE FROM airlinecompany WHERE airlinecompany_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompanyId);
            int result = ps.executeUpdate();

            if (result > 0) {
                System.out.println("✅ 航空公司删除成功: " + airlinecompanyId);
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 删除航空公司失败: " + e.getMessage());
            throw new RuntimeException("删除航空公司失败: " + e.getMessage());
        }
    }

    /**
     * 检查航空公司ID是否已存在
     * @param airlinecompanyId 航空公司ID
     * @return 是否存在
     */
    public boolean existsById(String airlinecompanyId) {
        String sql = "SELECT COUNT(*) FROM airlinecompany WHERE airlinecompany_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, airlinecompanyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ 检查航空公司ID是否存在失败: " + e.getMessage());
            throw new RuntimeException("检查航空公司ID是否存在失败: " + e.getMessage());
        }
    }

    /**
     * 获取航空公司总数
     * @return 航空公司总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM airlinecompany";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("❌ 统计航空公司数量失败: " + e.getMessage());
            throw new RuntimeException("统计航空公司数量失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存航空公司
     * @param airlinecompanies 航空公司列表
     * @return 成功保存的数量
     */
    public int batchSave(List<Airlinecompany> airlinecompanies) {
        String sql = "INSERT INTO airlinecompany (airlinecompany_ID, airlinecompany_name, airlinecompany_telephone) VALUES (?, ?, ?)";
        int savedCount = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Airlinecompany airlinecompany : airlinecompanies) {
                    ps.setString(1, airlinecompany.getAirlinecompanyId());
                    ps.setString(2, airlinecompany.getAirlinecompanyName());
                    ps.setString(3, airlinecompany.getAirlinecompanyTelephone());
                    ps.addBatch();
                }

                int[] results = ps.executeBatch();
                conn.commit(); // 提交事务

                for (int result : results) {
                    if (result > 0) savedCount++;
                }

                System.out.println("✅ 批量保存航空公司成功: " + savedCount + "/" + airlinecompanies.size());

            } catch (SQLException e) {
                conn.rollback(); // 回滚事务
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("❌ 批量保存航空公司失败: " + e.getMessage());
            throw new RuntimeException("批量保存航空公司失败: " + e.getMessage());
        }

        return savedCount;
    }

    /**
     * 将ResultSet映射为Airlinecompany对象
     * @param rs ResultSet对象
     * @return Airlinecompany对象
     * @throws SQLException SQL异常
     */
    private Airlinecompany mapResultSetToAirlinecompany(ResultSet rs) throws SQLException {
        Airlinecompany airlinecompany = new Airlinecompany();
        airlinecompany.setAirlinecompanyId(rs.getString("airlinecompany_ID"));
        airlinecompany.setAirlinecompanyName(rs.getString("airlinecompany_name"));
        airlinecompany.setAirlinecompanyTelephone(rs.getString("airlinecompany_telephone"));
        return airlinecompany;
    }
}