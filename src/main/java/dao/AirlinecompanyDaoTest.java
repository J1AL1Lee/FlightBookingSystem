package dao;
import model.Airlinecompany;

import java.util.List;

// 测试AirlinecompanyDao的使用
public class AirlinecompanyDaoTest {
    public static void main(String[] args) {
        AirlinecompanyDao dao = new AirlinecompanyDao();

        // 1. 保存航空公司
        Airlinecompany airline = new Airlinecompany("10012", "中华航空");
        airline.setAirlinecompanyTelephone("95557777");
        dao.save(airline);

        // 2. 查询航空公司
        Airlinecompany found = dao.findById("10012");
        System.out.println("查询结果: " + found);

        // 3. 查询所有
        List<Airlinecompany> all = dao.findAll();
        System.out.println("所有航空公司: " + all.size() + " 家");

        // 4. 模糊查询
        List<Airlinecompany> searchResult = dao.findByNameLike("中国");
        System.out.println("包含'中国'的航空公司: " + searchResult.size() + " 家");
    }
}
