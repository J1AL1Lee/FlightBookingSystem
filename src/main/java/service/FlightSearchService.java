package service;

import dao.FlightDao;
import dao.FlightrecordDao;
import dao.AirlinecompanyDao;
import dao.UserDao;
import model.Flight;
import model.Flightrecord;
import model.Airlinecompany;
import model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 航班搜索服务类 - 支持VIP折扣价格
 */
public class FlightSearchService {

    private FlightDao flightDao = new FlightDao();
    private FlightrecordDao flightrecordDao = new FlightrecordDao();
    private AirlinecompanyDao airlineDao = new AirlinecompanyDao();
    private UserDao userDao = new UserDao();

    /**
     * 根据航班ID获取航班信息
     * @param flightId 航班ID
     * @return 航班信息，未找到返回null
     */
    public Flight getFlightById(String flightId) {
        try {
            Flight flight = flightDao.findById(flightId);
            if (flight != null) {
                System.out.println("✅ 找到航班: " + flightId);
            } else {
                System.err.println("❌ 未找到航班: " + flightId);
            }
            return flight;
        } catch (Exception e) {
            System.err.println("❌ 查询航班失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 航班搜索结果DTO - 根据用户VIP状态显示不同价格
     */
    public static class FlightSearchResult {
        private Flight flight;              // 航班静态信息
        private Flightrecord flightrecord;  // 航程动态信息
        private Airlinecompany airline;     // 航空公司信息
        private boolean isVipUser;          // 是否VIP用户
        private int finalPrice0;            // 经济舱最终价格
        private int finalPrice1;            // 商务舱最终价格
        private int originalPrice0;         // 经济舱原价
        private int originalPrice1;         // 商务舱原价

        public FlightSearchResult(Flight flight, Flightrecord flightrecord, Airlinecompany airline, boolean isVipUser) {
            this.flight = flight;
            this.flightrecord = flightrecord;
            this.airline = airline;
            this.isVipUser = isVipUser;

            // 原价
            this.originalPrice0 = flight.getSeat0Price();
            this.originalPrice1 = flight.getSeat1Price();

            // 最终价格（VIP用户享受折扣，普通用户原价）
            if (isVipUser) {
                this.finalPrice0 = Math.round(flight.getSeat0Price() * flight.getDiscount());
                this.finalPrice1 = Math.round(flight.getSeat1Price() * flight.getDiscount());
            } else {
                this.finalPrice0 = flight.getSeat0Price();
                this.finalPrice1 = flight.getSeat1Price();
            }
        }

        // Getters
        public Flight getFlight() { return flight; }
        public Flightrecord getFlightrecord() { return flightrecord; }
        public Airlinecompany getAirline() { return airline; }
        public boolean isVipUser() { return isVipUser; }
        public int getFinalPrice0() { return finalPrice0; }
        public int getFinalPrice1() { return finalPrice1; }
        public int getOriginalPrice0() { return originalPrice0; }
        public int getOriginalPrice1() { return originalPrice1; }
        public float getDiscount() { return flight.getDiscount(); }

        // 便利方法
        public String getFlightId() { return flight.getFlightId(); }
        public String getAirportFrom() { return flight.getAirportFrom(); }
        public String getAirportTo() { return flight.getAirportTo(); }
        public LocalTime getTimeTakeoff() { return flight.getTimeTakeoff(); }
        public LocalTime getTimeArrive() { return flight.getTimeArrive(); }
        public LocalDate getFlightDate() { return flightrecord.getFlightDate(); }
        public int getSeat0Left() { return flightrecord.getSeat0Left(); }
        public int getSeat1Left() { return flightrecord.getSeat1Left(); }
        public String getAirlineName() { return airline.getAirlinecompanyName(); }

        /**
         * 获取价格显示信息
         * @param seatType 座位类型 (0-经济舱, 1-商务舱)
         * @return 价格显示字符串
         */
        public String getPriceDisplay(int seatType) {
            int originalPrice = (seatType == 0) ? originalPrice0 : originalPrice1;
            int finalPrice = (seatType == 0) ? finalPrice0 : finalPrice1;

            if (isVipUser && finalPrice < originalPrice) {
                return String.format("%d元 (原价%d元, VIP折扣%.1f折)",
                        finalPrice, originalPrice, getDiscount() * 10);
            } else {
                return finalPrice + "元";
            }
        }

        /**
         * 计算折扣金额
         * @param seatType 座位类型 (0-经济舱, 1-商务舱)
         * @return 折扣金额
         */
        public int getDiscountAmount(int seatType) {
            int originalPrice = (seatType == 0) ? originalPrice0 : originalPrice1;
            int finalPrice = (seatType == 0) ? finalPrice0 : finalPrice1;
            return originalPrice - finalPrice;
        }

        @Override
        public String toString() {
            String vipStatus = isVipUser ? " [VIP]" : "";
            return String.format("FlightSearchResult{%s %s→%s %s 经济舱:%s(%d座) 商务舱:%s(%d座)%s}",
                    getFlightId(), getAirportFrom(), getAirportTo(), getFlightDate(),
                    getPriceDisplay(0), getSeat0Left(), getPriceDisplay(1), getSeat1Left(), vipStatus);
        }
    }

    /**
     * 核心搜索方法：根据起降机场和日期搜索可用航班（需要用户ID判断VIP状态）
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param flightDate 航班日期
     * @param userId 用户ID（用于判断VIP状态）
     * @return 航班搜索结果列表
     */
    public List<FlightSearchResult> searchAvailableFlights(String airportFrom, String airportTo,
                                                           LocalDate flightDate, String userId) {
        System.out.println("🔍 搜索航班: " + airportFrom + " → " + airportTo + " " + flightDate +
                (userId != null ? " (用户: " + userId + ")" : " (游客模式)"));

        // 1. 判断用户VIP状态
        boolean isVipUser = checkVipStatus(userId);

        // 2. 查找符合航线的所有航班（静态信息）
        List<Flight> flights = flightDao.searchFlights(airportFrom, airportTo);

        List<FlightSearchResult> results = new ArrayList<>();

        // 3. 为每个航班查找对应日期的航程记录（动态信息）
        for (Flight flight : flights) {
            Flightrecord record = flightrecordDao.findByFlightAndDate(flight.getFlightId(), flightDate);

            // 只返回有航程记录的航班
            if (record != null) {
                // 4. 查找航空公司信息
                Airlinecompany airline = airlineDao.findById(flight.getAirlinecompanyId());

                // 5. 组合结果（根据VIP状态计算价格）
                results.add(new FlightSearchResult(flight, record, airline, isVipUser));
            }
        }

        String vipInfo = isVipUser ? " (VIP用户享受折扣价格)" : " (普通用户原价)";
        System.out.println("✅ 搜索结果: 找到 " + results.size() + " 个可用航班" + vipInfo);
        return results;
    }

    /**
     * 搜索有足够座位的航班
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param flightDate 航班日期
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param requiredSeats 需要的座位数
     * @param userId 用户ID
     * @return 有足够座位的航班列表
     */
    public List<FlightSearchResult> searchFlightsWithAvailableSeats(String airportFrom, String airportTo,
                                                                    LocalDate flightDate, int seatType,
                                                                    int requiredSeats, String userId) {
        System.out.println("🪑 搜索有座位的航班: " + airportFrom + " → " + airportTo + " " + flightDate +
                " 需要" + requiredSeats + "个" + (seatType == 0 ? "经济舱" : "商务舱") + "座位");

        List<FlightSearchResult> allFlights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        List<FlightSearchResult> availableFlights = new ArrayList<>();

        for (FlightSearchResult result : allFlights) {
            int availableSeats = (seatType == 0) ? result.getSeat0Left() : result.getSeat1Left();
            if (availableSeats >= requiredSeats) {
                availableFlights.add(result);
            }
        }

        System.out.println("✅ 有足够座位的航班: " + availableFlights.size() + " 个");
        return availableFlights;
    }

    /**
     * 按价格范围搜索航班（考虑VIP折扣）
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param flightDate 航班日期
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param userId 用户ID
     * @return 价格范围内的航班列表
     */
    public List<FlightSearchResult> searchFlightsByPrice(String airportFrom, String airportTo, LocalDate flightDate,
                                                         int minPrice, int maxPrice, int seatType, String userId) {
        System.out.println("💰 按价格搜索航班: " + airportFrom + " → " + airportTo + " " + flightDate +
                " 价格范围:" + minPrice + "-" + maxPrice + "元");

        List<FlightSearchResult> allFlights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        List<FlightSearchResult> priceMatchFlights = new ArrayList<>();

        for (FlightSearchResult result : allFlights) {
            int finalPrice = (seatType == 0) ? result.getFinalPrice0() : result.getFinalPrice1();
            if (finalPrice >= minPrice && finalPrice <= maxPrice) {
                priceMatchFlights.add(result);
            }
        }

        System.out.println("✅ 价格符合的航班: " + priceMatchFlights.size() + " 个");
        return priceMatchFlights;
    }

    /**
     * 查找最便宜的航班（考虑VIP折扣）
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param flightDate 航班日期
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param userId 用户ID
     * @return 最便宜的航班，如果没有返回null
     */
    public FlightSearchResult findCheapestFlight(String airportFrom, String airportTo, LocalDate flightDate,
                                                 int seatType, String userId) {
        System.out.println("💰 查找最便宜航班: " + airportFrom + " → " + airportTo + " " + flightDate);

        List<FlightSearchResult> flights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        FlightSearchResult cheapest = null;
        int minPrice = Integer.MAX_VALUE;

        for (FlightSearchResult result : flights) {
            // 只考虑有座位的航班
            int availableSeats = (seatType == 0) ? result.getSeat0Left() : result.getSeat1Left();
            if (availableSeats > 0) {
                int price = (seatType == 0) ? result.getFinalPrice0() : result.getFinalPrice1();
                if (price < minPrice) {
                    minPrice = price;
                    cheapest = result;
                }
            }
        }

        if (cheapest != null) {
            System.out.println("✅ 最便宜航班: " + cheapest.getFlightId() + " 价格:" +
                    cheapest.getPriceDisplay(seatType));
        }
        return cheapest;
    }

    /**
     * VIP专享查询：显示能享受折扣的航班
     * @param airportFrom 起飞机场
     * @param airportTo 到达机场
     * @param flightDate 航班日期
     * @param userId 用户ID
     * @return VIP折扣航班列表
     */
    public List<FlightSearchResult> searchVipDiscountFlights(String airportFrom, String airportTo,
                                                             LocalDate flightDate, String userId) {
        if (!checkVipStatus(userId)) {
            System.out.println("⚠️ 非VIP用户，无法查询VIP专享折扣航班");
            return new ArrayList<>();
        }

        System.out.println("👑 搜索VIP专享折扣航班: " + airportFrom + " → " + airportTo + " " + flightDate);

        List<FlightSearchResult> allFlights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        List<FlightSearchResult> discountFlights = new ArrayList<>();

        for (FlightSearchResult result : allFlights) {
            // 只返回有折扣的航班（折扣小于1.0）
            if (result.getDiscount() < 1.0f) {
                discountFlights.add(result);
            }
        }

        System.out.println("✅ VIP专享折扣航班: " + discountFlights.size() + " 个");
        return discountFlights;
    }

    /**
     * 预订座位（考虑VIP价格）
     * @param flightrecordId 航程ID
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param seatCount 座位数量
     * @param userId 用户ID
     * @return 预订结果信息
     */


    /**
     * 检查用户VIP状态
     * @param userId 用户ID
     * @return 是否为VIP用户
     */
    private boolean checkVipStatus(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false; // 游客用户
        }

        try {
            User user = userDao.findByUserId(userId);
            if (user != null) {
                boolean isVip = "是".equals(user.getVipState());
                System.out.println("👤 用户 " + userId + " VIP状态: " + (isVip ? "VIP用户" : "普通用户"));
                return isVip;
            }
        } catch (Exception e) {
            System.err.println("❌ 查询用户VIP状态失败: " + e.getMessage());
        }

        return false; // 默认非VIP
    }

    /**
     * 预订结果类
     */

    /**
     * 取消预订
     * @param flightrecordId 航程ID
     * @param seatType 座位类型 (0-经济舱, 1-商务舱)
     * @param seatCount 座位数量
     * @return 是否取消成功
     */
    public boolean cancelBooking(String flightrecordId, int seatType, int seatCount) {
        return flightrecordDao.cancelSeats(flightrecordId, seatType, seatCount);
    }

    /**
     * 为指定日期创建所有航班的航程记录
     * @param flightDate 航班日期
     * @return 创建成功的航程记录数量
     */
    public int initializeFlightrecordsForDate(LocalDate flightDate) {
        System.out.println("📅 为日期 " + flightDate + " 初始化航程记录");

        List<Flight> allFlights = flightDao.findAll();
        List<Flightrecord> records = new ArrayList<>();

        for (Flight flight : allFlights) {
            // 检查是否已有该日期的航程记录
            if (!flightrecordDao.existsByFlightAndDate(flight.getFlightId(), flightDate)) {
                String recordId = FlightrecordDao.generateFlightrecordId(flight.getFlightId(), flightDate);

                Flightrecord record = new Flightrecord();
                record.setFlightrecordId(recordId);
                record.setFlightId(flight.getFlightId());
                record.setFlightDate(flightDate);
                record.setSeat0Left(flight.getSeat0Capacity()); // 初始剩余座位 = 总座位数
                record.setSeat1Left(flight.getSeat1Capacity());

                records.add(record);
            }
        }

        if (!records.isEmpty()) {
            int saved = flightrecordDao.batchSave(records);
            System.out.println("✅ 为 " + flightDate + " 创建了 " + saved + " 条航程记录");
            return saved;
        } else {
            System.out.println("ℹ️ " + flightDate + " 的航程记录已存在，无需创建");
            return 0;
        }
    }
}