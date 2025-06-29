package service;

import model.Order;
import model.Flightrecord;
import model.Flight;
import model.User;
import dao.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.List;

public class BookingService {
    private OrderDao orderDao = new OrderDao();
    private FlightrecordDao FlightrecordDao = new FlightrecordDao();
    private FlightSearchService flightSearchService = new FlightSearchService();
    private UserDao userDao = new UserDao();

    /**
     * 创建订单主流程
     * @param flightrecordId 航班记录ID
     * @param userId 用户ID
     * @param seatType 座位类型（0经济舱，1商务舱）
     * @return 订单ID，失败返回null
     */
    public String createBooking(String flightrecordId, String userId, Integer seatType) {
        System.out.println("🎫 开始创建订单: 用户=" + userId + ", 航班记录=" + flightrecordId + ", 座位类型=" + seatType);

        try {
            // 1. 验证参数
            if (!validateBookingParams(flightrecordId, userId, seatType)) {
                System.err.println("❌ 订票参数验证失败");
                return null;
            }

            // 2. 检查座位余量
            if (!checkSeatAvailability(flightrecordId, seatType)) {
                System.err.println("❌ 座位不足，无法预订");
                return null;
            }

            // 3. 锁定座位（减少余量）
            if (!lockSeat(flightrecordId, seatType)) {
                System.err.println("❌ 座位锁定失败，可能被其他用户抢购");
                return null;
            }

            // 4. 生成订单
            String orderId = generateUniqueOrderId();
            Order order = createOrderObject(orderId, flightrecordId, userId, seatType);

            if (order == null) {
                // 创建订单对象失败，释放座位
                releaseSeat(flightrecordId, seatType);
                return null;
            }

            // 5. 保存订单
            try {
                String savedOrderId = orderDao.save(order);
                System.out.println("✅ 订单创建成功: " + savedOrderId);
                return savedOrderId;
            } catch (Exception e) {
                // 保存失败，释放座位
                System.err.println("❌ 保存订单失败，释放座位");
                releaseSeat(flightrecordId, seatType);
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ 创建订单异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 验证订票参数
     */
    private boolean validateBookingParams(String flightrecordId, String userId, Integer seatType) {
        if (flightrecordId == null || flightrecordId.trim().isEmpty()) {
            System.err.println("❌ 航班记录ID不能为空");
            return false;
        }

        if (userId == null || userId.length() != 6) {
            System.err.println("❌ 用户ID格式错误，应为6位数字");
            return false;
        }

        if (seatType == null || (seatType != 0 && seatType != 1)) {
            System.err.println("❌ 座位类型错误，应为0（经济舱）或1（商务舱）");
            return false;
        }

        // 检查用户是否存在
        try {
            User user = userDao.findByUserId(userId);
            if (user == null) {
                System.err.println("❌ 用户不存在: " + userId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ 验证用户失败: " + e.getMessage());
            return false;
        }

        // 检查航班记录是否存在
        try {
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                System.err.println("❌ 航班记录不存在: " + flightrecordId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ 验证航班记录失败: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 检查座位可用性
     */
    public boolean checkSeatAvailability(String flightrecordId, Integer seatType) {
        try {
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                return false;
            }

            int availableSeats = seatType == 0 ? record.getSeat0Left() : record.getSeat1Left();
            boolean available = availableSeats > 0;

            System.out.println("🔍 座位检查: " + (seatType == 0 ? "经济舱" : "商务舱") +
                    " 剩余=" + availableSeats + "座 " + (available ? "✅可预订" : "❌已售罄"));

            return available;

        } catch (Exception e) {
            System.err.println("❌ 检查座位可用性失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 计算订单价格
     */
    public Integer calculateOrderPrice(String flightrecordId, Integer seatType, String userId) {
        try {
            System.out.println("💰 开始计算价格...");

            // 1. 获取航班记录
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                System.err.println("❌ 航班记录不存在");
                return null;
            }

            // 2. 获取航班基础价格
            Flight flight = flightSearchService.getFlightById(record.getFlightId());
            if (flight == null) {
                System.err.println("❌ 航班信息不存在");
                return null;
            }

            Integer basePrice = seatType == 0 ? flight.getSeat0Price() : flight.getSeat1Price();
            System.out.println("📊 基础价格: ¥" + basePrice);

            // 3. 检查用户VIP状态
            User user = userDao.findByUserId(userId);
            boolean isVip = user != null && "是".equals(user.getVipState());

            // 4. 应用折扣
            Integer finalPrice;
            if (isVip) {
                finalPrice = Math.round(basePrice * flight.getDiscount());
                System.out.println("👑 VIP用户享受折扣: " + (flight.getDiscount() * 100) + "% 最终价格: ¥" + finalPrice);
            } else {
                finalPrice = basePrice;
                System.out.println("👤 普通用户价格: ¥" + finalPrice);
            }

            return finalPrice;

        } catch (Exception e) {
            System.err.println("❌ 计算价格失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成唯一订单ID
     */
    public String generateUniqueOrderId() {
        String orderId;
        int attempts = 0;
        do {
            orderId = generateOrderId();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("生成唯一订单ID失败，尝试次数过多");
            }
        } while (orderDao.isOrderIdExists(orderId));

        return orderId;
    }

    /**
     * 生成订单ID
     */
    private String generateOrderId() {
        // 格式：年月日 + 4位随机数，如：20251229XXXX
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        String randomNum = String.format("%04d", random.nextInt(10000));
        return date + randomNum;
    }

    /**
     * 生成座位号
     */
    public Integer generateSeatNumber() {
        // 简单实现：随机生成1-200的座位号
        return new Random().nextInt(200) + 1;
    }

    /**
     * 创建订单对象
     */
    private Order createOrderObject(String orderId, String flightrecordId, String userId, Integer seatType) {
        try {
            // 获取航班记录获取flightId和日期
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                System.err.println("❌ 无法获取航班记录");
                return null;
            }

            Order order = new Order();
            order.setOrderId(orderId);
            order.setUserId(userId);
            order.setFlightId(record.getFlightId());
            order.setOrderState("未支付");
            order.setFlightTime(record.getFlightDate());
            order.setOrderTime(LocalDateTime.now());
            order.setSeatId(generateSeatNumber());
            order.setSeatType(seatType);

            System.out.println("📝 订单对象创建成功: " + order.toString());
            return order;

        } catch (Exception e) {
            System.err.println("❌ 创建订单对象失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 锁定座位（原子操作）
     */
    public boolean lockSeat(String flightrecordId, Integer seatType) {
        try {
            boolean success = FlightrecordDao.bookSeats(flightrecordId, seatType,1);
            if (success) {
                System.out.println("🔒 座位锁定成功: " + flightrecordId + " " + (seatType == 0 ? "经济舱" : "商务舱"));
            } else {
                System.err.println("❌ 座位锁定失败: 可能座位不足");
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ 锁定座位异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 释放座位
     */
    public boolean releaseSeat(String flightrecordId, Integer seatType) {
        try {
            boolean success = FlightrecordDao.cancelSeats(flightrecordId, seatType,1);
            if (success) {
                System.out.println("🔓 座位释放成功: " + flightrecordId + " " + (seatType == 0 ? "经济舱" : "商务舱"));
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ 释放座位异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(String orderId, String userId) {
        System.out.println("🚫 开始取消订单: " + orderId);

        try {
            // 1. 验证订单归属
            if (!orderDao.isOrderOwnedByUser(orderId, userId)) {
                System.err.println("❌ 订单验证失败: 订单不属于该用户");
                return false;
            }

            // 2. 获取订单信息
            Order order = orderDao.findById(orderId);
            if (order == null) {
                System.err.println("❌ 订单不存在: " + orderId);
                return false;
            }

            if (!"未支付".equals(order.getOrderState())) {
                System.err.println("❌ 订单状态错误: " + order.getOrderState() + "，只能取消未支付订单");
                return false;
            }

            // 3. 释放座位
            String flightrecordId = findFlightRecordId(order.getFlightId(), order.getFlightTime());
            if (flightrecordId != null) {
                releaseSeat(flightrecordId, order.getSeatType());
            } else {
                System.err.println("⚠️ 找不到对应的航班记录，无法释放座位");
            }

            // 4. 删除订单
            boolean success = orderDao.deleteOrder(orderId);
            if (success) {
                System.out.println("✅ 订单取消成功: " + orderId);
            }
            return success;

        } catch (Exception e) {
            System.err.println("❌ 取消订单失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据航班ID和日期查找记录ID
     */
    private String findFlightRecordId(String flightId, LocalDate flightDate) {
        try {
            return FlightrecordDao.getFlightRecordId(flightId, flightDate);
        } catch (Exception e) {
            System.err.println("❌ 查找航班记录ID失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取用户订单列表
     */
    public List<Order> getUserOrders(String userId) {
        try {
            return orderDao.findByUserId(userId);
        } catch (Exception e) {
            System.err.println("❌ 获取用户订单失败: " + e.getMessage());
            throw new RuntimeException("获取用户订单失败", e);
        }
    }

    /**
     * 获取订单详情
     */
    public Order getOrderById(String orderId) {
        try {
            return orderDao.findById(orderId);
        } catch (Exception e) {
            System.err.println("❌ 获取订单详情失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 确认订单（支付成功后调用）
     */
    public boolean confirmOrder(String orderId) {
        try {
            boolean success = orderDao.updateOrderState(orderId, "已完成");
            if (success) {
                System.out.println("✅ 订单确认成功: " + orderId);
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ 确认订单失败: " + e.getMessage());
            return false;
        }
    }
}