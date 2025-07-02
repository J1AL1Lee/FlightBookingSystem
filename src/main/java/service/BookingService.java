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

    // 🆕 添加 PaymentService
    private PaymentService paymentService = new PaymentService();

    /**
     * 创建订单主流程（原版本 - 直接设为已支付）
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

                // 🆕 6. 创建支付记录（因为订单状态直接设为已支付，所以同时创建支付记录）
                Integer orderPrice = calculateOrderPrice(flightrecordId, seatType, userId);
                if (orderPrice != null) {
                    String paymentId = paymentService.createPayment(savedOrderId, orderPrice, "在线支付");
                    if (paymentId != null) {
                        // 直接处理为支付成功
                        paymentService.processPaymentSuccess(paymentId);
                        System.out.println("💳 支付记录创建并处理成功: " + paymentId);
                    } else {
                        System.err.println("⚠️ 支付记录创建失败，但订单已创建");
                    }
                }

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
     * 🆕 创建订单（分步版本 - 先创建订单，后支付）
     */
    public String createBookingWithoutPayment(String flightrecordId, String userId, Integer seatType) {
        System.out.println("🎫 开始创建订单（待支付）: 用户=" + userId + ", 航班记录=" + flightrecordId + ", 座位类型=" + seatType);

        try {
            // 验证、检查、锁定座位（同原逻辑）
            if (!validateBookingParams(flightrecordId, userId, seatType) ||
                    !checkSeatAvailability(flightrecordId, seatType) ||
                    !lockSeat(flightrecordId, seatType)) {
                return null;
            }

            // 生成订单
            String orderId = generateUniqueOrderId();
            Order order = createOrderObjectWithoutPayment(orderId, flightrecordId, userId, seatType);

            if (order == null) {
                releaseSeat(flightrecordId, seatType);
                return null;
            }

            // 保存订单
            String savedOrderId = orderDao.save(order);
            System.out.println("✅ 订单创建成功（待支付）: " + savedOrderId);
            return savedOrderId;

        } catch (Exception e) {
            System.err.println("❌ 创建订单异常: " + e.getMessage());
            releaseSeat(flightrecordId, seatType);
            return null;
        }
    }

    /**
     * 🆕 为订单创建支付
     */
    public String payForOrder(String orderId, String userId, String payMethod) {
        try {
            // 验证订单
            Order order = orderDao.findById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                System.err.println("❌ 订单验证失败");
                return null;
            }

            if (!"未支付".equals(order.getOrderState())) {
                System.err.println("❌ 订单状态错误: " + order.getOrderState());
                return null;
            }

            // 计算支付金额
            String flightrecordId = findFlightRecordId(order.getFlightId(), order.getFlightTime());
            Integer amount = calculateOrderPrice(flightrecordId, order.getSeatType(), userId);
            if (amount == null) {
                System.err.println("❌ 计算支付金额失败");
                return null;
            }

            // 创建支付记录
            String paymentId = paymentService.createPayment(orderId, amount, payMethod);
            System.out.println("💳 支付记录创建: " + paymentId);
            return paymentId;

        } catch (Exception e) {
            System.err.println("❌ 订单支付失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🆕 申请退款
     */
    public String requestRefund(String orderId, String userId, String reason) {
        try {
            // 验证订单
            Order order = orderDao.findById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                System.err.println("❌ 订单验证失败");
                return null;
            }

            if (!"已支付".equals(order.getOrderState())) {
                System.err.println("❌ 订单状态不允许退款: " + order.getOrderState());
                return null;
            }

            // 申请退款
            String refundId = paymentService.requestRefund(orderId, reason);
            if (refundId != null) {
                System.out.println("💰 退款申请成功: " + refundId);

                // 释放座位
                String flightrecordId = findFlightRecordId(order.getFlightId(), order.getFlightTime());
                if (flightrecordId != null) {
                    releaseSeat(flightrecordId, order.getSeatType());
                    System.out.println("🪑 座位已释放");
                }
            }

            return refundId;

        } catch (Exception e) {
            System.err.println("❌ 申请退款失败: " + e.getMessage());
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
        // 🔧 修改为8位格式：月日 + 4位随机数，如：07014523
        String monthDay = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
        Random random = new Random();
        String randomNum = String.format("%04d", random.nextInt(10000));
        return monthDay + randomNum;
    }

    /**
     * 生成座位ID - 按座位余量依次分配
     * @param flightrecordId 航班记录ID
     * @param seatType 座位类型（0经济舱，1商务舱）
     * @return 座位ID（数字），从1开始依次分配
     */
    public Integer generateSeatId(String flightrecordId, Integer seatType) {
        try {
            System.out.println("🪑 开始生成座位ID: 航班记录=" + flightrecordId + ", 座位类型=" +
                    (seatType == 0 ? "经济舱" : "商务舱"));

            // 1. 获取航班记录信息
            Flightrecord flightrecord = FlightrecordDao.findById(flightrecordId);
            if (flightrecord == null) {
                System.err.println("❌ 航班记录不存在: " + flightrecordId);
                return null;
            }

            // 2. 获取航班基础信息（座位容量）
            Flight flight = flightSearchService.getFlightById(flightrecord.getFlightId());
            if (flight == null) {
                System.err.println("❌ 航班信息不存在: " + flightrecord.getFlightId());
                return null;
            }

            // 3. 计算座位分配信息
            int totalCapacity;    // 总座位数
            int leftSeats;        // 剩余座位数

            if (seatType == 0) {
                // 经济舱
                totalCapacity = flight.getSeat0Capacity();
                leftSeats = flightrecord.getSeat0Left();
            } else {
                // 商务舱
                totalCapacity = flight.getSeat1Capacity();
                leftSeats = flightrecord.getSeat1Left();
            }

            // 4. 计算下一个应分配的座位ID
            int assignedSeats = totalCapacity - leftSeats;  // 已分配座位数
            int nextSeatId = assignedSeats + 1;             // 下一个座位ID

            // 5. 验证座位分配的合理性
            if (nextSeatId > totalCapacity) {
                System.err.println("❌ 座位分配异常: 下一个座位ID(" + nextSeatId +
                        ") > 总容量(" + totalCapacity + ")");
                return null;
            }

            if (leftSeats <= 0) {
                System.err.println("❌ 座位已满: 剩余座位=" + leftSeats);
                return null;
            }

            System.out.println("✅ 座位ID生成成功: " + nextSeatId +
                    " (总容量=" + totalCapacity +
                    ", 剩余=" + leftSeats +
                    ", 已分配=" + assignedSeats + ")");

            return nextSeatId;

        } catch (Exception e) {
            System.err.println("❌ 生成座位ID失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建订单对象（原版本 - 直接设为已支付）
     */
    private Order createOrderObject(String orderId, String flightrecordId, String userId, Integer seatType) {
        try {
            // 获取航班记录获取flightId和日期
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                System.err.println("❌ 无法获取航班记录");
                return null;
            }

            // 生成座位ID
            Integer seatId = generateSeatId(flightrecordId, seatType);
            if (seatId == null) {
                System.err.println("❌ 生成座位ID失败");
                return null;
            }

            Order order = new Order();
            order.setOrderId(orderId);
            order.setUserId(userId);
            order.setFlightId(record.getFlightId());
            order.setOrderState("正常");  // 保持原有逻辑 - 直接设为已支付
            order.setFlightTime(record.getFlightDate());
            order.setOrderTime(LocalDateTime.now());
            order.setSeatId(seatId);  // 使用生成的座位ID
            order.setSeatType(seatType);

            System.out.println("📝 订单对象创建成功: " + order.toString());
            return order;

        } catch (Exception e) {
            System.err.println("❌ 创建订单对象失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🆕 创建订单对象（未支付版本）
     */
    private Order createOrderObjectWithoutPayment(String orderId, String flightrecordId, String userId, Integer seatType) {
        try {
            // 获取航班记录获取flightId和日期
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                System.err.println("❌ 无法获取航班记录");
                return null;
            }

            // 生成座位ID
            Integer seatId = generateSeatId(flightrecordId, seatType);
            if (seatId == null) {
                System.err.println("❌ 生成座位ID失败");
                return null;
            }

            Order order = new Order();
            order.setOrderId(orderId);
            order.setUserId(userId);
            order.setFlightId(record.getFlightId());
            order.setOrderState("未支付");  // 设为未支付状态
            order.setFlightTime(record.getFlightDate());
            order.setOrderTime(LocalDateTime.now());
            order.setSeatId(seatId);  // 使用生成的座位ID
            order.setSeatType(seatType);

            System.out.println("📝 订单对象创建成功（未支付）: " + order.toString());
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
     * 取消订单（保持原有逻辑，🆕 添加支付记录处理）
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

            // 🆕 3. 处理支付记录
            try {
                if (paymentService.getPaymentByOrderId(orderId) != null) {
                    String paymentId = paymentService.getPaymentByOrderId(orderId).getPayId();
                    paymentService.cancelPayment(paymentId);
                    System.out.println("💳 支付记录已取消");
                }
            } catch (Exception e) {
                System.err.println("⚠️ 取消支付记录时出错: " + e.getMessage());
                // 不影响订单取消流程，继续执行
            }

            // 4. 释放座位
            String flightrecordId = findFlightRecordId(order.getFlightId(), order.getFlightTime());
            if (flightrecordId != null) {
                releaseSeat(flightrecordId, order.getSeatType());
            } else {
                System.err.println("⚠️ 找不到对应的航班记录，无法释放座位");
            }

            // 5. 删除订单
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

    // 🆕 新增方法：获取订单的支付信息
    public String getOrderPaymentInfo(String orderId, String userId) {
        try {
            // 验证订单归属
            Order order = orderDao.findById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                System.err.println("❌ 订单验证失败");
                return null;
            }

            // 获取支付记录
            return paymentService.getPaymentByOrderId(orderId) != null ?
                    paymentService.getPaymentByOrderId(orderId).getPayId() : null;

        } catch (Exception e) {
            System.err.println("❌ 获取订单支付信息失败: " + e.getMessage());
            return null;
        }
    }
    /**
     * 🆕 创建未支付状态的订单（为 BookingHandler 新流程服务）
     * @param flightrecordId 航班记录ID
     * @param userId 用户ID
     * @param seatType 座位类型（0经济舱，1商务舱）
     * @return 订单ID，失败返回null
     */
    public String createUnpaidBooking(String flightrecordId, String userId, Integer seatType) {
        System.out.println("🎫 开始创建未支付订单: 用户=" + userId + ", 航班记录=" + flightrecordId + ", 座位类型=" + seatType);

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

            // 4. 生成订单ID
            String orderId = generateUniqueOrderId();

            // 5. 创建未支付订单对象
            Order order = createUnpaidOrderObject(orderId, flightrecordId, userId, seatType);

            if (order == null) {
                // 创建订单对象失败，释放座位
                releaseSeat(flightrecordId, seatType);
                return null;
            }

            // 6. 保存订单
            try {
                String savedOrderId = orderDao.save(order);
                System.out.println("✅ 未支付订单创建成功: " + savedOrderId);
                return savedOrderId;
            } catch (Exception e) {
                // 保存失败，释放座位
                System.err.println("❌ 保存订单失败，释放座位: " + e.getMessage());
                releaseSeat(flightrecordId, seatType);
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ 创建未支付订单异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 🆕 更新订单状态
     * @param orderId 订单ID
     * @param newStatus 新状态
     * @return 是否更新成功
     */
    public boolean updateOrderStatus(String orderId, String newStatus) {
        System.out.println("🔄 更新订单状态: " + orderId + " -> " + newStatus);

        try {
            // 1. 验证订单是否存在
            Order order = orderDao.findById(orderId);
            if (order == null) {
                System.err.println("❌ 订单不存在: " + orderId);
                return false;
            }

            // 2. 验证状态转换的合理性
            String currentStatus = order.getOrderState();
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                System.err.println("❌ 无效的状态转换: " + currentStatus + " -> " + newStatus);
                return false;
            }

            // 3. 执行状态更新
            boolean success = orderDao.updateOrderState(orderId, newStatus);

            if (success) {
                System.out.println("✅ 订单状态更新成功: " + orderId + " -> " + newStatus);
            } else {
                System.err.println("❌ 订单状态更新失败: " + orderId);
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ 更新订单状态异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 🆕 创建未支付订单对象
     * @param orderId 订单ID
     * @param flightrecordId 航班记录ID
     * @param userId 用户ID
     * @param seatType 座位类型
     * @return 订单对象
     */
    private Order createUnpaidOrderObject(String orderId, String flightrecordId, String userId, Integer seatType) {
        try {
            // 获取航班记录获取flightId和日期
            Flightrecord record = FlightrecordDao.findById(flightrecordId);
            if (record == null) {
                System.err.println("❌ 无法获取航班记录");
                return null;
            }

            // 生成座位ID
            Integer seatId = generateSeatId(flightrecordId, seatType);
            if (seatId == null) {
                System.err.println("❌ 生成座位ID失败");
                return null;
            }

            Order order = new Order();
            order.setOrderId(orderId);
            order.setUserId(userId);
            order.setFlightId(record.getFlightId());
            order.setOrderState("未支付");  // 🎯 关键：设为未支付状态
            order.setFlightTime(record.getFlightDate());
            order.setOrderTime(LocalDateTime.now());
            order.setSeatId(seatId);
            order.setSeatType(seatType);

            System.out.println("📝 未支付订单对象创建成功: " + order.toString());
            return order;

        } catch (Exception e) {
            System.err.println("❌ 创建未支付订单对象失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🆕 验证订单状态转换是否合理
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 是否为合理的状态转换
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // 定义合理的状态转换规则
        switch (currentStatus) {
            case "未支付":
                // 未支付可以转换为：正常、已取消
                return "正常".equals(newStatus) || "已取消".equals(newStatus);

            case "正常":
                // 正常可以转换为：已完成、已取消、已退款
                return "已完成".equals(newStatus) || "已取消".equals(newStatus) || "已退款".equals(newStatus);

            case "已完成":
                // 已完成可以转换为：已退款
                return "已退款".equals(newStatus);

            case "已取消":
            case "已退款":
                // 已取消和已退款是终态，不能再转换
                return false;

            default:
                System.err.println("❌ 未知的订单状态: " + currentStatus);
                return false;
        }
    }

    // 🆕 获取 PaymentService 实例（供外部调用支付相关功能）
    public PaymentService getPaymentService() {
        return paymentService;
    }
}